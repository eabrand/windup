package org.jboss.windup.rules.apps.xml;

import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.service.GraphService;

public class NamespaceService extends GraphService<NamespaceMetaModel>
{

    public NamespaceService()
    {
        super(NamespaceMetaModel.class);
    }

    public NamespaceService(GraphContext context)
    {
        super(context, NamespaceMetaModel.class);
    }

    public NamespaceMetaModel createNamespaceSchemaLocation(String namespaceURI, String schemaLocation)
    {
        Iterable<NamespaceMetaModel> results = getGraphContext().getQuery().type(NamespaceMetaModel.class)
                    .has("namespaceURI", namespaceURI).has("schemaLocation", schemaLocation)
                    .vertices(NamespaceMetaModel.class);

        for (NamespaceMetaModel result : results)
        {
            return result;
        }

        // otherwise, create it.
        NamespaceMetaModel meta = getGraphContext().getFramed().addVertex(null, NamespaceMetaModel.class);
        meta.setSchemaLocation(schemaLocation);
        meta.setURI(namespaceURI);

        return meta;
    }

}
