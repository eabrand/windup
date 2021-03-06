package org.jboss.windup.reporting.model;

import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.model.resource.FileModel;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Extends the file model with some convenience accessors for getting to {@link InlineHintModel} and other reporting
 * related data.
 */
@TypeValue("ReportFileModel")
public interface ReportFileModel extends FileModel
{
    /**
     * Get all {@link InlineHintModel} instances attached to this {@link ReportFileModel}
     */
    @GremlinGroovy(frame = true, value = "it.in(\"" + FileReferenceModel.FILE_MODEL
                + "\").has(\"" + WindupVertexFrame.TYPE_PROP
                + "\", com.thinkaurelius.titan.core.attribute.Text.CONTAINS, \"" + InlineHintModel.TYPE + "\")")
    public Iterable<InlineHintModel> getInlineHints();

    /**
     * Get all {@link ClassificationModel} instances attached to this {@link ReportFileModel}
     */
    @Adjacency(label = ClassificationModel.FILE_MODEL, direction = Direction.IN)
    public Iterable<ClassificationModel> getClassificationModels();
}
