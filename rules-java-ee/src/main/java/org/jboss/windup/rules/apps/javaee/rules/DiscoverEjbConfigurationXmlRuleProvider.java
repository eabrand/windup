package org.jboss.windup.rules.apps.javaee.rules;

import static org.joox.JOOX.$;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.jboss.windup.config.GraphRewrite;
import org.jboss.windup.config.RulePhase;
import org.jboss.windup.config.WindupRuleProvider;
import org.jboss.windup.config.operation.GraphOperation;
import org.jboss.windup.config.operation.ruleelement.AbstractIterationOperation;
import org.jboss.windup.config.query.Query;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.service.GraphService;
import org.jboss.windup.graph.service.Service;
import org.jboss.windup.rules.apps.java.model.JavaClassModel;
import org.jboss.windup.rules.apps.java.service.JavaClassService;
import org.jboss.windup.rules.apps.javaee.model.EjbDeploymentDescriptorModel;
import org.jboss.windup.rules.apps.javaee.model.EjbEntityBeanModel;
import org.jboss.windup.rules.apps.javaee.model.EjbMessageDrivenModel;
import org.jboss.windup.rules.apps.javaee.model.EjbSessionBeanModel;
import org.jboss.windup.rules.apps.javaee.model.EnvironmentReferenceModel;
import org.jboss.windup.rules.apps.javaee.service.EnvironmentReferenceService;
import org.jboss.windup.rules.apps.xml.DiscoverXmlFilesRuleProvider;
import org.jboss.windup.rules.apps.xml.DoctypeMetaModel;
import org.jboss.windup.rules.apps.xml.NamespaceMetaModel;
import org.jboss.windup.rules.apps.xml.XmlFileModel;
import org.jboss.windup.rules.apps.xml.XmlFileService;
import org.jboss.windup.util.xml.DoctypeUtils;
import org.jboss.windup.util.xml.NamespaceUtils;
import org.ocpsoft.rewrite.config.ConditionBuilder;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Discovers ejb-jar.xml files and parses the related metadata
 * 
 * @author jsightler <jesse.sightler@gmail.com>
 * 
 */
public class DiscoverEjbConfigurationXmlRuleProvider extends WindupRuleProvider
{
    private static final Logger LOG = Logger.getLogger(DiscoverEjbConfigurationXmlRuleProvider.class.getSimpleName());

    private static final String dtdRegex = "(?i).*enterprise.javabeans.*";

    @Inject
    private EnvironmentReferenceService environmentReferenceService;
    @Inject
    private JavaClassService javaClassService;
    @Inject
    private XmlFileService xmlFileService;

    @Override
    public RulePhase getPhase()
    {
        return RulePhase.DISCOVERY;
    }

    @Override
    public List<Class<? extends WindupRuleProvider>> getExecuteAfter()
    {
        return asClassList(DiscoverXmlFilesRuleProvider.class);
    }

    @Override
    public Configuration getConfiguration(GraphContext context)
    {
        ConditionBuilder ejbJarXmlFound = Query
                    .find(XmlFileModel.class)
                    .withProperty(XmlFileModel.ROOT_TAG_NAME, "ejb-jar");

        GraphOperation addEjbMetadata = new ExtractEJBMetadata();

        return ConfigurationBuilder.begin()
                    .addRule()
                    .when(ejbJarXmlFound)
                    .perform(addEjbMetadata);
    }

    private class ExtractEJBMetadata extends AbstractIterationOperation<XmlFileModel>
    {
        @Override
        public void perform(GraphRewrite event, EvaluationContext context, XmlFileModel payload)
        {
            Document doc = xmlFileService.loadDocumentQuiet(payload);
            if (doc == null)
            {
                // failed to parse, skip
                return;
            }

            extractMetadata(event.getGraphContext(), payload, doc);
        }
    }

    private void extractMetadata(GraphContext context, XmlFileModel xmlModel, Document doc)
    {
        // otherwise, it is a EJB-JAR XML.
        if (xmlModel.getDoctype() != null)
        {
            // check doctype.
            if (!processDoctypeMatches(xmlModel.getDoctype()))
            {
                // move to next document.
                return;
            }
            String version = processDoctypeVersion(xmlModel.getDoctype());
            extractMetadata(context, xmlModel, doc, version);
        }
        else
        {
            String namespace = $(doc).find("ejb-jar").namespaceURI();
            if (StringUtils.isBlank(namespace))
            {
                namespace = doc.getFirstChild().getNamespaceURI();
            }

            String version = $(doc).attr("version");

            // if the version attribute isn't found, then grab it from the XSD name if we can.
            if (StringUtils.isBlank(version))
            {
                for (NamespaceMetaModel ns : xmlModel.getNamespaces())
                {
                    if (StringUtils.equals(ns.getURI(), namespace))
                    {
                        version = NamespaceUtils.extractVersion(ns.getSchemaLocation());
                    }
                }
            }

            extractMetadata(context, xmlModel, doc, version);
        }
    }

    private void extractMetadata(GraphContext ctx, XmlFileModel xml, Document doc, String versionInformation)
    {
        // check the root XML node.
        EjbDeploymentDescriptorModel facet = GraphService.addTypeToModel(ctx, xml, EjbDeploymentDescriptorModel.class);

        if (StringUtils.isNotBlank(versionInformation))
        {
            facet.setSpecificationVersion(versionInformation);
        }

        // process all session beans...
        //
        for (Element element : $(doc).find("session").get())
        {
            processSessionBeanElement(ctx, facet, element);
        }

        // process all message driven beans...
        for (Element element : $(doc).find("message-driven").get())
        {
            processMessageDrivenElement(ctx, facet, element);
        }

        // process all entity beans...
        for (Element element : $(doc).find("entity").get())
        {
            processEntityElement(ctx, facet, element);
        }
    }

    private boolean processDoctypeMatches(DoctypeMetaModel entry)
    {
        if (StringUtils.isNotBlank(entry.getPublicId()))
        {
            if (Pattern.matches(dtdRegex, entry.getPublicId()))
            {
                return true;
            }
        }

        if (StringUtils.isNotBlank(entry.getSystemId()))
        {
            if (Pattern.matches(dtdRegex, entry.getSystemId()))
            {
                return true;
            }

        }
        return false;
    }

    private String processDoctypeVersion(DoctypeMetaModel entry)
    {
        String publicId = entry.getPublicId();
        String systemId = entry.getSystemId();

        // extract the version information from the public / system ID.
        String versionInformation = DoctypeUtils.extractVersion(publicId, systemId);
        return versionInformation;
    }

    private void processSessionBeanElement(GraphContext ctx, EjbDeploymentDescriptorModel ejbConfig, Element element)
    {
        JavaClassModel home = null;
        JavaClassModel localHome = null;
        JavaClassModel remote = null;
        JavaClassModel local = null;
        JavaClassModel ejb = null;

        String ejbId = extractAttributeAndTrim(element, "id");
        String displayName = extractChildTagAndTrim(element, "display-name");
        String ejbName = extractChildTagAndTrim(element, "ejb-name");

        // get local class.
        String localClz = extractChildTagAndTrim(element, "local");
        if (localClz != null)
        {
            local = javaClassService.getOrCreate(localClz);
        }

        // get local home class.
        String localHomeClz = extractChildTagAndTrim(element, "local-home");
        if (localHomeClz != null)
        {
            localHome = javaClassService.getOrCreate(localHomeClz);
        }

        // get home class.
        String homeClz = extractChildTagAndTrim(element, "home");
        if (homeClz != null)
        {
            home = javaClassService.getOrCreate(homeClz);
        }

        // get remote class.
        String remoteClz = extractChildTagAndTrim(element, "remote");
        if (remoteClz != null)
        {
            remote = javaClassService.getOrCreate(remoteClz);
        }

        // get the ejb class.
        String ejbClz = extractChildTagAndTrim(element, "ejb-class");
        if (ejbClz != null)
        {
            ejb = javaClassService.getOrCreate(ejbClz);
        }

        String sessionType = extractChildTagAndTrim(element, "session-type");
        String transactionType = extractChildTagAndTrim(element, "transaction-type");

        Service<EjbSessionBeanModel> sessionBeanService = ctx.getService(EjbSessionBeanModel.class);
        EjbSessionBeanModel sessionBean = sessionBeanService.create();
        sessionBean.setEjbId(ejbId);
        sessionBean.setDisplayName(displayName);
        sessionBean.setBeanName(ejbName);
        sessionBean.setEjbLocal(local);
        sessionBean.setEjbLocalHome(localHome);
        sessionBean.setEjbHome(home);
        sessionBean.setEjbRemote(remote);
        sessionBean.setEjbClass(ejb);
        sessionBean.setSessionType(sessionType);
        sessionBean.setTransactionType(transactionType);

        List<EnvironmentReferenceModel> refs = processEnvironmentReference(element);
        for (EnvironmentReferenceModel ref : refs)
        {
            sessionBean.addEnvironmentReference(ref);
        }

        ejbConfig.addEjbSessionBean(sessionBean);
    }

    private void processMessageDrivenElement(GraphContext ctx, EjbDeploymentDescriptorModel ejbConfig, Element element)
    {
        JavaClassModel ejb = null;

        String ejbId = extractAttributeAndTrim(element, "id");
        String displayName = extractChildTagAndTrim(element, "display-name");
        String ejbName = extractChildTagAndTrim(element, "ejb-name");

        // get the ejb class.
        String ejbClz = extractChildTagAndTrim(element, "ejb-class");
        if (ejbClz != null)
        {
            ejb = javaClassService.getOrCreate(ejbClz);
        }

        String sessionType = extractChildTagAndTrim(element, "session-type");
        String transactionType = extractChildTagAndTrim(element, "transaction-type");

        Service<EjbMessageDrivenModel> sessionBeanService = ctx.getService(EjbMessageDrivenModel.class);
        EjbMessageDrivenModel mdb = sessionBeanService.create();
        mdb.setEjbClass(ejb);
        mdb.setBeanName(ejbName);
        mdb.setDisplayName(displayName);
        mdb.setEjbId(ejbId);
        mdb.setSessionType(sessionType);
        mdb.setTransactionType(transactionType);

        List<EnvironmentReferenceModel> refs = processEnvironmentReference(element);
        for (EnvironmentReferenceModel ref : refs)
        {
            mdb.addEnvironmentReference(ref);
        }

        ejbConfig.addMessageDriven(mdb);
    }

    private void processEntityElement(GraphContext ctx, EjbDeploymentDescriptorModel ejbConfig, Element element)
    {
        JavaClassModel localHome = null;
        JavaClassModel local = null;
        JavaClassModel ejb = null;

        String ejbId = extractAttributeAndTrim(element, "id");
        String displayName = extractChildTagAndTrim(element, "display-name");
        String ejbName = extractChildTagAndTrim(element, "ejb-name");

        // get local class.
        String localClz = extractChildTagAndTrim(element, "local");
        if (localClz != null)
        {
            local = javaClassService.getOrCreate(localClz);
        }

        // get local home class.
        String localHomeClz = extractChildTagAndTrim(element, "local-home");
        if (localHomeClz != null)
        {
            localHome = javaClassService.getOrCreate(localHomeClz);
        }

        // get the ejb class.
        String ejbClz = extractChildTagAndTrim(element, "ejb-class");
        if (ejbClz != null)
        {
            ejb = javaClassService.getOrCreate(ejbClz);
        }

        String persistenceType = extractChildTagAndTrim(element, "persistence-type");

        // create new entity facet.
        Service<EjbEntityBeanModel> ejbEntityService = ctx.getService(EjbEntityBeanModel.class);
        EjbEntityBeanModel entity = ejbEntityService.create();
        entity.setPersistenceType(persistenceType);
        entity.setEjbId(ejbId);
        entity.setDisplayName(displayName);
        entity.setBeanName(ejbName);
        entity.setEjbClass(ejb);
        entity.setEjbLocalHome(localHome);
        entity.setEjbLocal(local);

        List<EnvironmentReferenceModel> refs = processEnvironmentReference(element);
        for (EnvironmentReferenceModel ref : refs)
        {
            entity.addEnvironmentReference(ref);
        }

        ejbConfig.addEjbEntityBean(entity);
    }

    private List<EnvironmentReferenceModel> processEnvironmentReference(Element element)
    {
        List<EnvironmentReferenceModel> resources = new LinkedList<EnvironmentReferenceModel>();

        // find JMS references...
        List<Element> queueReferences = $(element).find("resource-env-ref").get();
        for (Element e : queueReferences)
        {
            String type = $(e).child("resource-env-ref-type").text();
            String name = $(e).child("resource-env-ref-name").text();

            type = StringUtils.trim(type);
            name = StringUtils.trim(name);

            EnvironmentReferenceModel ref = environmentReferenceService.findEnvironmentReference(name, type);
            if (ref == null)
            {
                ref = environmentReferenceService.create();
                ref.setName(name);
                ref.setReferenceType(type);
            }
            resources.add(ref);
        }

        return resources;
    }

    private String extractAttributeAndTrim(Element element, String property)
    {
        String result = $(element).attr(property);
        return StringUtils.trimToNull(result);
    }

    private String extractChildTagAndTrim(Element element, String property)
    {
        String result = $(element).find(property).first().text();
        return StringUtils.trimToNull(result);
    }
}
