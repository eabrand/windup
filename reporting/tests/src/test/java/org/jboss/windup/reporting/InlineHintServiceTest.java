package org.jboss.windup.reporting;

import java.io.File;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.ProjectModel;
import org.jboss.windup.graph.model.resource.FileModel;
import org.jboss.windup.reporting.model.InlineHintModel;
import org.jboss.windup.reporting.service.InlineHintService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class InlineHintServiceTest
{
    @Deployment
    @Dependencies({
                @AddonDependency(name = "org.jboss.windup.config:windup-config"),
                @AddonDependency(name = "org.jboss.windup.graph:windup-graph"),
                @AddonDependency(name = "org.jboss.windup.reporting:windup-reporting"),
                @AddonDependency(name = "org.jboss.forge.furnace.container:cdi")
    })
    public static ForgeArchive getDeployment()
    {
        ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
                    .addBeansXML()
                    .addAsResource(new File("src/test/resources/reports"))
                    .addAsAddonDependencies(
                                AddonDependencyEntry.create("org.jboss.windup.config:windup-config"),
                                AddonDependencyEntry.create("org.jboss.windup.graph:windup-graph"),
                                AddonDependencyEntry.create("org.jboss.windup.reporting:windup-reporting"),
                                AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi")
                    );
        return archive;
    }

    @Inject
    private GraphContext context;

    @Test
    public void testHintEffort() throws Exception
    {
        InlineHintService inlineHintService = new InlineHintService(context);

        ProjectModel projectModel = fillData();
        int totalEffort = inlineHintService.getMigrationEffortPoints(projectModel, true);
        Assert.assertEquals(150, totalEffort);
    }

    private ProjectModel fillData()
    {
        InlineHintService inlineHintService = new InlineHintService(context);

        FileModel f1 = context.getFramed().addVertex(null, FileModel.class);
        f1.setFilePath("/f1");
        FileModel f2 = context.getFramed().addVertex(null, FileModel.class);
        f2.setFilePath("/f2");

        InlineHintModel b1 = inlineHintService.create();
        InlineHintModel b1b = inlineHintService.create();
        b1.setFile(f1);
        b1.setEffort(50);
        b1b.setFile(f1);
        b1b.setEffort(100);

        ProjectModel projectModel = context.getFramed().addVertex(null, ProjectModel.class);
        projectModel.addFileModel(f1);
        f1.setProjectModel(projectModel);
        projectModel.addFileModel(f2);
        f2.setProjectModel(projectModel);

        return projectModel;
    }
}
