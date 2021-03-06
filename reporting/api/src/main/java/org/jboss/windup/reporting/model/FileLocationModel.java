package org.jboss.windup.reporting.model;

import org.jboss.windup.graph.model.resource.FileModel;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue(FileLocationModel.TYPE)
public interface FileLocationModel extends FileReferenceModel
{

    String TYPE = "fileLocationModel";
    String PROPERTY_LINE_NUMBER = "lineNumber";
    String PROPERTY_LENGTH = "length";
    String PROPERTY_COLUMN_NUMBER = "startPosition";

    /**
     * Set the line number at which this {@link InlineHintModel} should appear in the designated {@link FileModel}.
     */
    @Property(PROPERTY_LINE_NUMBER)
    public void setLineNumber(int lineNumber);

    /**
     * Get the line number at which this {@link InlineHintModel} should appear in the designated {@link FileModel}.
     */
    @Property(PROPERTY_LINE_NUMBER)
    public int getLineNumber();

    /**
     * Set the column number at which this {@link InlineHintModel} should appear in the designated {@link FileModel}.
     */
    @Property(PROPERTY_COLUMN_NUMBER)
    public void setColumnNumber(int startPosition);

    /**
     * Get the column number at which this {@link InlineHintModel} should appear in the designated {@link FileModel}.
     */
    @Property(PROPERTY_COLUMN_NUMBER)
    public int getColumnNumber();

    /**
     * Set the length of content for which this {@link InlineHintModel} should cover in the designated {@link FileModel}
     * .
     */
    @Property(PROPERTY_LENGTH)
    public void setLength(int length);

    /**
     * Get the length of content for which this {@link InlineHintModel} should cover in the designated {@link FileModel}
     * .
     */
    @Property(PROPERTY_LENGTH)
    public int getLength();

}
