package mcp.aot;


import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * provides hints for MCP interactions. 
 * 
 * @author Josh Long 
 */
class McpHints implements RuntimeHintsRegistrar {


    private Set<TypeReference> innerClasses(Class<?> clzz) {
        var indent = new HashSet<String>();
        this.findNestedClasses(clzz, indent);
        return indent.stream().map(TypeReference::of).collect(Collectors.toSet());
    }

    private void findNestedClasses(Class<?> clazz, Set<String> indent) {
        var classes = new ArrayList<Class<?>>();
        classes.addAll(Arrays.asList(clazz.getDeclaredClasses()));
        classes.addAll(Arrays.asList(clazz.getClasses()));
        for (var nestedClass : classes) {
            this.findNestedClasses(nestedClass, indent);
        }
        indent.addAll(classes.stream().map(Class::getName).toList());
    }

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        var mcs = MemberCategory.values();
        for (var tr : innerClasses(McpSchema.class))
            hints.reflection().registerType(tr, mcs);
    }
}
