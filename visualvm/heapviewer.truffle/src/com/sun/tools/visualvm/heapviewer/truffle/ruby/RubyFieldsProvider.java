/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.tools.visualvm.heapviewer.truffle.ruby;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyPlugin;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObjectPropertyProvider;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewPlugin;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import java.util.Collection;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=HeapViewerNode.Provider.class, position = 210)
public class RubyFieldsProvider extends TruffleObjectPropertyProvider.Fields<RubyObject> {
    
    public RubyFieldsProvider() {
        super("variables", RubyObject.class, true);
    }
    
    
    @Override
    public boolean supportsView(Heap heap, String viewID) {
        return viewID.startsWith("ruby_");
    }

    @Override
    public boolean supportsNode(HeapViewerNode node, Heap heap, String viewID) {
        return node instanceof RubyNodes.RubyNode && !(node instanceof RubyNodes.RubyObjectReferenceNode);
    }

    @Override
    protected boolean isLanguageObject(Instance instance) {
        return RubyObject.isRubyObject(instance);
    }

    @Override
    protected RubyObject createObject(Instance instance) {
        return new RubyObject(instance);
    }

    @Override
    protected HeapViewerNode createObjectFieldNode(RubyObject object, String type, FieldValue field) {
        return new RubyNodes.RubyObjectFieldNode(object, type, field);
    }
    
    @Override
    protected Collection<FieldValue> getPropertyItems(RubyObject object, Heap heap) {
        List<FieldValue> fields = new ArrayList();
        
        fields.addAll(object.getFieldValues());
        fields.addAll(object.getStaticFieldValues());
        
        return fields;
    }
    
    @Override
    protected boolean includeInstance(Instance instance) {
        String className = instance.getJavaClass().getName();
        
        if (className.startsWith("java.lang.") ||
            className.startsWith("org.truffleruby.core.rope."))
            return true;
        
        return false;
    }
    
    
    @ServiceProvider(service=HeapViewPlugin.Provider.class, position = 200)
    public static class PluginProvider extends HeapViewPlugin.Provider {

        public HeapViewPlugin createPlugin(HeapContext context, HeapViewerActions actions, String viewID) {
            if (!RubyHeapFragment.isRubyHeap(context)) return null;
            
            Lookup.getDefault().lookupAll(HeapViewerNode.Provider.class);
            RubyFieldsProvider fieldsProvider = Lookup.getDefault().lookup(RubyFieldsProvider.class);
            
            return new TruffleObjectPropertyPlugin("Variables", "Variables", Icons.getIcon(ProfilerIcons.NODE_FORWARD), "ruby_objects_fields", context, actions, fieldsProvider);
        }
        
    }
    
}