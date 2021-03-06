package org.jboss.windup.graph.service;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.jboss.windup.graph.FramedElementInMemory;
import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.graph.model.InMemoryVertexFrame;
import org.jboss.windup.graph.model.WindupConfigurationModel;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.graph.service.exception.NonUniqueResultException;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.Text;
import com.thinkaurelius.titan.util.datastructures.IterablesUtil;
import com.tinkerpop.blueprints.GraphQuery;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraphQuery;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;
import com.tinkerpop.gremlin.java.GremlinPipeline;

public class GraphService<T extends WindupVertexFrame> implements Service<T>
{
    @Inject
    private GraphContext context;

    private Class<T> type;

    protected GraphService(Class<T> type)
    {
        this.type = type;
    }

    public GraphService(GraphContext context, Class<T> type)
    {
        this(type);
        this.context = context;
    }

    public static synchronized WindupConfigurationModel getConfigurationModel(GraphContext context)
    {
        WindupConfigurationModel config = new GraphService<>(context, WindupConfigurationModel.class).getUnique();
        if (config == null)
            config = new GraphService<>(context, WindupConfigurationModel.class).create();
        return config;
    }

    @Override
    public void commit()
    {
        this.context.getGraph().getBaseGraph().commit();
    }

    @Override
    public long count(Iterable<?> obj)
    {
        GremlinPipeline<Iterable<?>, Object> pipe = new GremlinPipeline<Iterable<?>, Object>();
        return pipe.start(obj).count();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T createInMemory()
    {
        Class<?>[] resolvedTypes = new Class<?>[] { VertexFrame.class, InMemoryVertexFrame.class, type };
        return (T) Proxy.newProxyInstance(this.type.getClassLoader(),
                    resolvedTypes, new FramedElementInMemory<>(context, this.type));
    }

    /**
     * Create a new instance of the given {@link WindupVertexFrame} type.
     */
    @Override
    public T create()
    {
        return context.getFramed().addVertex(null, type);
    }

    @Override
    public T create(Object id)
    {
        return context.getFramed().addVertex(id, type);
    }

    public void delete(T frame)
    {
        context.getFramed().removeVertex(frame.asVertex());
    }

    @Override
    public T addTypeToModel(WindupVertexFrame model)
    {
        return GraphService.addTypeToModel(getGraphContext(), model, type);
    }

    protected FramedGraphQuery findAllQuery()
    {
        return context.getQuery().type(type);
    }

    @Override
    public Iterable<T> findAll()
    {
        return (Iterable<T>) findAllQuery().vertices(type);
    }

    @Override
    public Iterable<T> findAllByProperties(String[] keys, String[] vals)
    {
        FramedGraphQuery fgq = findAllQuery();

        for (int i = 0, j = keys.length; i < j; i++)
        {
            String key = keys[i];
            String val = vals[i];

            fgq = fgq.has(key, val);
        }

        return fgq.vertices(type);
    }

    @Override
    public Iterable<T> findAllByProperty(String key, Object value)
    {
        return context.getFramed().getVertices(key, value, type);
    }

    @Override
    public Iterable<T> findAllByPropertyMatchingRegex(String key, String... regex)
    {
        if (regex.length == 0)
            return IterablesUtil.emptyIterable();

        final String regexFinal;
        if (regex.length == 1)
        {
            regexFinal = regex[0];
        }
        else
        {
            StringBuilder builder = new StringBuilder();
            builder.append("\\b(");
            int i = 0;
            for (String value : regex)
            {
                if (i > 0)
                    builder.append("|");
                builder.append(value);
                i++;
            }
            builder.append(")\\b");
            regexFinal = builder.toString();
        }
        return findAllQuery().has(key, Text.REGEX, regexFinal).vertices(type);
    }

    @Override
    public T getById(Object id)
    {
        return context.getFramed().getVertex(id, type);
    }

    protected T frame(Vertex vertex)
    {
        return getGraphContext().getFramed().frame(vertex, this.getType());
    }

    @Override
    public Class<T> getType()
    {
        return type;
    }

    protected GraphQuery getTypedQuery()
    {
        return getGraphContext().getQuery().type(type);
    }

    protected String getTypeValueForSearch()
    {
        TypeValue typeValue = type.getAnnotation(TypeValue.class);
        if (typeValue == null)
            throw new IllegalArgumentException("Must contain annotation 'TypeValue'");
        return typeValue.value();
    }

    @Override
    public T getUnique() throws NonUniqueResultException
    {
        Iterable<T> results = findAll();

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<T> iter = results.iterator();
        T result = iter.next();

        if (iter.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        return result;
    }

    @Override
    public T getUniqueByProperty(String property, Object value) throws NonUniqueResultException
    {
        Iterable<T> results = findAllByProperty(property, value);

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<T> iter = results.iterator();
        T result = iter.next();

        if (iter.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        return result;
    }

    protected T getUnique(GraphQuery framedQuery)
    {
        Iterable<Vertex> results = framedQuery.vertices();

        if (!results.iterator().hasNext())
        {
            return null;
        }

        Iterator<Vertex> iter = results.iterator();
        Vertex result = iter.next();

        if (iter.hasNext())
        {
            throw new NonUniqueResultException("Expected unique value, but returned non-unique.");
        }

        return frame(result);
    }

    protected GraphContext getGraphContext()
    {
        return context;
    }

    @Override
    public TitanTransaction newTransaction()
    {
        return context.getGraph().getBaseGraph().newTransaction();
    }

    public static List<WindupVertexFrame> toVertexFrames(GraphContext graphContext, Iterable<Vertex> vertices)
    {
        List<WindupVertexFrame> results = new ArrayList<>();
        for (Vertex v : vertices)
        {
            WindupVertexFrame frame = graphContext.getFramed().frame(v, WindupVertexFrame.class);
            results.add(frame);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public static <T extends WindupVertexFrame> List<T> toVertexFrames(GraphContext graphContext,
                Iterable<Vertex> vertices, Class<T> frameType)
    {
        List<T> results = new ArrayList<>();
        for (Vertex v : vertices)
        {
            WindupVertexFrame frame = graphContext.getFramed().frame(v, WindupVertexFrame.class);
            if (frameType.isAssignableFrom(frame.getClass()))
                results.add((T) frame);
            else
                throw new IllegalStateException("Expected frame type [" + frameType.getName() + "] but was "
                            + frame.getClass().getInterfaces() + ".");
        }
        return results;
    }

    /**
     * Adds the specified type to this frame, and returns a new object that implements this type.
     * 
     * @see GraphTypeManagerTest
     */
    public static <T extends WindupVertexFrame> T addTypeToModel(GraphContext graphContext, WindupVertexFrame frame,
                Class<T> type)
    {
        Vertex vertex = frame.asVertex();
        graphContext.getGraphTypeRegistry().addTypeToElement(type, vertex);
        return graphContext.getFramed().frame(vertex, type);
    }

    @Override
    public void remove(T model)
    {
        model.asVertex().remove();
    }

}
