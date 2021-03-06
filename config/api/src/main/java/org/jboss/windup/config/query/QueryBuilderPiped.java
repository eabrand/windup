package org.jboss.windup.config.query;

import org.jboss.windup.graph.model.WindupVertexFrame;
import org.ocpsoft.rewrite.config.ConditionBuilder;

import com.tinkerpop.gremlin.groovy.Gremlin;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface QueryBuilderPiped extends ConditionBuilder
{
    /**
     * Query the selected {@link WindupVertexFrame} instances via {@link Gremlin}. This method can be used to change the
     * type of the resulting variable (named {@link #as(String)}).
     */
    public QueryBuilderPiped piped(QueryGremlinCriterion pipe);

    /**
     * Set the name of the output variable into which results of the {@link Query} will be stored.
     */
    ConditionBuilder as(String name);
}
