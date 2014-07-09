package org.jboss.windup.ext.groovy.java.events;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.jboss.windup.graph.GraphContext;
import org.jboss.windup.rules.apps.java.groovy.blacklist.GroovyBlackListSupport;
import org.jboss.windup.rules.apps.java.groovy.blacklist.GroovyBlackListSupportRegex;
import org.jboss.windup.rules.apps.java.scan.ast.event.JavaScannerASTEvent;

@Singleton
public class GroovyJavaASTEventService
{
    private static List<GroovyBlackListSupport> groovyBlackListSupport = new ArrayList<>();

    public void onJavaScannerASTEvent(@Observes JavaScannerASTEvent event)
    {
        for (GroovyBlackListSupport support : groovyBlackListSupport)
        {
            support.evaluateBlackList(event);
        }
    }

    public void registerInterest(GraphContext graphContext, String ruleID, String regexPattern, String hint)
    {
        GroovyBlackListSupportRegex support = new GroovyBlackListSupportRegex(graphContext, hint, ruleID, regexPattern);
        groovyBlackListSupport.add(support);
    }
}