/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.windup.config;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.enterprise.inject.Vetoed;

import org.jboss.windup.config.exception.IllegalTypeArgumentException;
import org.jboss.windup.config.operation.Iteration;
import org.jboss.windup.graph.model.WindupVertexFrame;

/**
 * A variables stack - keeps few layers of "key"->[vertices] maps, one per rule execution level, {@link Iteration} and
 * {@link RuleSubset}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@Vetoed
public class Variables
{
    private final Deque<Map<String, Iterable<WindupVertexFrame>>> deque = new LinkedList<>();

    private Variables()
    {
    }

    /**
     * Gets an instance from a OCP rewrite context.
     */
    public static Variables instance(GraphRewrite event)
    {
        Variables instance = (Variables) event.getRewriteContext().get(Variables.class);
        if (instance == null)
        {
            instance = new Variables();
            event.getRewriteContext().put(Variables.class, instance);
        }
        return instance;
    }

    /**
     * Add new {@link Variables} layer on top of the stack.
     */
    public void push()
    {
        Map<String, Iterable<WindupVertexFrame>> newFrame = new HashMap<>();
        deque.push(newFrame);
    }

    /**
     * Remove the top {@link Variables} layer from the the stack.
     */
    public Map<String, Iterable<WindupVertexFrame>> pop()
    {
        return deque.pop();
    }

    /**
     * Get the top {@link Variables} layer from the stack.
     */
    public Map<String, Iterable<WindupVertexFrame>> peek()
    {
        return deque.peek();
    }

    /**
     * Type-safe wrapper around setVariable which sets only one framed vertex.
     */
    public void setSingletonVariable(String string, WindupVertexFrame frame)
    {
        setVariable(string, Collections.singletonList(frame));
    }

    /**
     * Set a variable in the top variables layer to given "collection" of the vertex frames. Can't be reassigned -
     * throws on attempt to reassign.
     */
    public void setVariable(String name, Iterable<WindupVertexFrame> frames)
    {
        Map<String, Iterable<WindupVertexFrame>> frame = peek();
        if (!Iteration.DEFAULT_VARIABLE_LIST_STRING.equals(name) && findVariable(name) != null)
        {
            throw new IllegalArgumentException("Variable \"" + name
                        + "\" has already been assigned and cannot be reassigned");
        }

        frame.put(name, frames);
    }
    
    /**
     * Remove a variable in the top variables layer. 
     */
    public void removeVariable(String name)
    {
        Map<String, Iterable<WindupVertexFrame>> frame = peek();
        frame.remove(name);
    }

    /**
     * Wrapper around {@link #findVariable(String)} which gives only one framed vertex, and checks if there is 0 or 1;
     * throws otherwise.
     */
    @SuppressWarnings("unchecked")
    public <T extends WindupVertexFrame> T findSingletonVariable(String name)
    {
        Iterable<WindupVertexFrame> frames = findVariable(name);
        if (null == frames)
        {
            throw new IllegalStateException("Variable not found: \"" + name + "\"");
        }

        Iterator<WindupVertexFrame> iterator = frames.iterator();
        if (!iterator.hasNext())
        {
            return null;
        }

        WindupVertexFrame obj = iterator.next();

        if (iterator.hasNext())
        {
            throw new IllegalStateException("More than one frame present "
                        + "under presumed singleton variable: " + name);
        }
        return (T) obj;
    }

    /**
     * Type-safe wrapper around {@link #findVariable(String)} returns a unique {@link WindupVertexFrame}.
     * 
     * @throws IllegalStateException If more than one frame was found.
     */
    @SuppressWarnings("unchecked")
    public <FRAMETYPE extends WindupVertexFrame> FRAMETYPE findSingletonVariable(Class<FRAMETYPE> type, String name)
    {
        WindupVertexFrame frame = findSingletonVariable(name);

        if (type != null && !type.isAssignableFrom(frame.getClass()))
        {
            throw new IllegalTypeArgumentException(name, type, frame.getClass());
        }

        return (FRAMETYPE) frame;
    }

    /**
     * Searches the variables layers, top to bottom, for given name, and returns if found; null otherwise.
     */
    public Iterable<WindupVertexFrame> findVariable(String name)
    {
        Iterator<Map<String, Iterable<WindupVertexFrame>>> descIter = deque.descendingIterator();
        Iterable<WindupVertexFrame> result = null;
        while (descIter.hasNext())
        {
            Map<String, Iterable<WindupVertexFrame>> frame = descIter.next();
            result = frame.get(name);
            if (result != null)
            {
                break;
            }
        }
        return result;
    }

    /**
     * Searches the variables layers, top to bottom, for the iterable having all of it's items of the given type. Return
     * null if not found.
     */
    public Iterable<WindupVertexFrame> findVariableOfType(Class<?> type)
    {
        Iterator<Map<String, Iterable<WindupVertexFrame>>> descIter = deque.descendingIterator();
        while (descIter.hasNext())
        {
            Map<String, Iterable<WindupVertexFrame>> topOfStack = descIter.next();
            for (Iterable<WindupVertexFrame> frames : topOfStack.values())
            {
                for (WindupVertexFrame frame : frames)
                {
                    if (!type.isAssignableFrom(frame.getClass()))
                    {
                        continue;
                    }
                }
                // now we know all the frames are of the chosen type
                return frames;
            }
        }
        return null;
    }
}
