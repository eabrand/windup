package org.jboss.windup.engine.visitor;

import org.apache.commons.lang.StringUtils;
import org.jboss.windup.engine.WindupContext;
import org.jboss.windup.engine.visitor.base.EmptyGraphVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Goes through an archive adding the archive entries to the graph.
 * 
 * @author bradsdavis
 *
 */
public class DebugVisitor extends EmptyGraphVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(DebugVisitor.class);
	
	private String typeValue;
	public DebugVisitor(Class<?> type) {
		TypeValue value = type.getAnnotation(TypeValue.class);
		this.typeValue = value.value();
	}
	
	public DebugVisitor() {
		
	}
	
	@Override
	public void visitContext(WindupContext context) {
		TitanGraph graph = context.getGraphContext().getGraph();
		
		Iterable<Vertex> vertices;
		if(StringUtils.isNotBlank(typeValue)) {
			LOG.info("Loading: " + typeValue);
			vertices = graph.getVertices("type", typeValue);
		}
		else {
			vertices = graph.getVertices();
		}
		
		for(Vertex v : vertices) {
			LOG.info("Vertex["+v+"]");
			for(String key : v.getPropertyKeys()) {
				LOG.info("  - key["+key+"] => "+v.getProperty(key));
			}
			for(Edge edge : v.query().direction(Direction.IN).edges()) {
				LOG.info("  - edge["+edge.getLabel()+", IN]");
			}
			for(Edge edge : v.query().direction(Direction.OUT).edges()) {
				LOG.info("  - edge["+edge.getLabel()+", OUT]");
			}
		}
	}
}