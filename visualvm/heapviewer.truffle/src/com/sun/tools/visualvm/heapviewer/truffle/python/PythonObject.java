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
package com.sun.tools.visualvm.heapviewer.truffle.python;

import com.sun.tools.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.utils.HeapUtils;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.Type;
import org.netbeans.lib.profiler.heap.Value;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 */
public class PythonObject extends TruffleObject.InstanceBased {

//    public static final DataType<PythonObject> DATA_TYPE = new DataType<PythonObject>(PythonObject.class, null, null);

    static final String PYTHON_OBJECT_FQN = "com.oracle.graal.python.builtins.objects.object.PythonObject"; // NOI18N
    static final String PYTHON_LIST_FQN = "com.oracle.graal.python.builtins.objects.list.PList"; // NOI18N
    static final String TREEMAP_ENTRY_FQN = "java.util.TreeMap$Entry";  // NOI18N
    static final String TREEMAP_FQN = "java.util.TreeMap";  // NOI18N

    private final Instance instance;
    private final Instance storage;
    private final Instance store;
    private final Instance dictStorage;
    private final ObjectArrayInstance array;
    private final Instance map;
    private final Instance set;
    private final Instance pythonClass;
    private String listType;
    private boolean isPrimitiveList;
    
    private String type;
    
    
    public PythonObject(Instance instance) {
        this(null, instance);
    }

    public PythonObject(String type, Instance instance) {
        if (instance == null) throw new IllegalArgumentException("Instance cannot be null");
        
        this.instance = instance;
        this.type = type;
        
        Object[] values = HeapUtils.getValuesOfFields(instance, "storage", "pythonClass", "store", "array", "map", "set", "dictStorage");
        
        storage = (Instance) values[0]; // NOI18N
        pythonClass = (Instance) values[1]; // NOI18N
        store = (Instance) values[2]; // NOI18N
        array = (ObjectArrayInstance) values[3]; // NOI18N
        map = (Instance) values[4]; // NOI18N
        set = (Instance) values[5]; // NOI18N
        dictStorage = (Instance) values[6]; // NOI18N
        
//        Map values = HeapUtils.getValuesOfFields(instance, "storage", "pythonClass", "store", "array", "map", "set", "dictStorage");
//        
//        storage = (Instance) values.get("storage"); // NOI18N
//        pythonClass = (Instance) values.get("pythonClass"); // NOI18N
//        store = (Instance) values.get("store"); // NOI18N
//        array = (ObjectArrayInstance) values.get("array"); // NOI18N
//        map = (Instance) values.get("map"); // NOI18N
//        set = (Instance) values.get("set"); // NOI18N
//        dictStorage = (Instance) values.get("dictStorage"); // NOI18N
        
//        storage = (Instance) instance.getValueOfField("storage"); // NOI18N
//        pythonClass = (Instance) instance.getValueOfField("pythonClass"); // NOI18N
//        store = (Instance) instance.getValueOfField("store"); // NOI18N
//        array = (ObjectArrayInstance) instance.getValueOfField("array"); // NOI18N
//        map = (Instance) instance.getValueOfField("map"); // NOI18N
//        set = (Instance) instance.getValueOfField("set"); // NOI18N
//        dictStorage = (Instance) instance.getValueOfField("dictStorage"); // NOI18N
    }

    public static boolean isPythonObject(Instance rObj) {
        return isSubClassOf(rObj, PYTHON_OBJECT_FQN);
    }

    List<FieldValue> getItems() {
        if (store != null || array != null) {
            return getListFields();
        }
        return Collections.EMPTY_LIST;
    }

    List<FieldValue> getAttributes() {
        if (map != null) {
            return getMapFields();
        }
        if (set != null) {
            return getSetFields();
        }
        if (dictStorage != null) {
            return getDictFields();
        }
        return new DynamicObject(storage).getFieldValues();
    }

    @Override
    public Instance getInstance() {
        return instance;
    }

    @Override
    public String getType(Heap heap) {
        if (type == null) type = DetailsUtils.getInstanceString(pythonClass, null);
        return type;
    }
    
    static String getPythonType(Instance instance) {
        return DetailsUtils.getInstanceString((Instance) instance.getValueOfField("pythonClass"), null);
    }

    @Override
    public long getSize() {
        long size = instance.getSize();
        if (storage != null) {
            size += storage.getSize();
        }
        if (store != null) {
            size += store.getSize();
        }
        if (array != null) {
            size += array.getSize();
        }
        return size;
    }
    
    @Override
    public long getRetainedSize() {
        return instance.getRetainedSize();
    }

    List<FieldValue> getReferences() {
        List<Value> refs = instance.getReferences();
        List<FieldValue> robjRefs = new ArrayList();

        for (Value ref : refs) {
            Instance defInstance = ref.getDefiningInstance();
            if (ref instanceof ArrayItemValue) {
                if (defInstance instanceof ObjectArrayInstance) {
                    List<Value> arrRefs = defInstance.getReferences();

                    for (Value arrRef : arrRefs) {
                        Instance pInstance = arrRef.getDefiningInstance();

                        if (PythonObject.isPythonObject(pInstance)) {
                            addItem(pInstance, ref, robjRefs);
                        } else {
                            Instance store = getReference(pInstance, PYTHON_OBJECT_FQN, "store");
                            if (PythonObject.isPythonObject(store)) {
                                addItem(store, ref, robjRefs);
                            }
                        }
                        addAttribute(pInstance, robjRefs);
                    }
                }
            }
            if (defInstance != null && defInstance.getJavaClass().getName().equals(TREEMAP_ENTRY_FQN)) {
                FieldValue rootReference = findRootPMap(defInstance);

                robjRefs.add(rootReference);
            }
            addAttribute(defInstance, robjRefs);
        }
        return robjRefs;
    }

    private void addItem(Instance pInstance, Value ref, List<FieldValue> robjRefs) {
        PythonObject pobject = new PythonObject(pInstance);
        int index = ((ArrayItemValue)ref).getIndex();
        List<FieldValue> items = pobject.getItems();

        if (index < items.size()) {
            FieldValue fv = items.get(index);
            if (fv instanceof ObjectFieldValue) {
                ObjectFieldValue ofv = (ObjectFieldValue) fv;
                if (instance.equals(ofv.getInstance())) {
                    robjRefs.add(fv);
                }
            }
        }
    }

    private FieldValue findRootPMap(Instance mapEntry) {
        for (Instance parent = getParentTreeEntry(mapEntry); parent != null; parent = getParentTreeEntry(parent)) {
            mapEntry = parent;
        }
        // top TreeMap$Entry
        Instance treeMap = getReference(mapEntry, TREEMAP_FQN, "root");
        Instance pythonObject = getReference(treeMap, PYTHON_OBJECT_FQN, "map");

        if (isPythonObject(pythonObject)) {
            for (FieldValue fv : new PythonObject(pythonObject).getAttributes()) {
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue ofv = (ObjectFieldValue) fv;
                    if (instance.equals(ofv.getInstance())) {
                        return fv;
                    }
                }
            }
        }
        return null;
    }

    private Instance getParentTreeEntry(Instance treeEntry) {
        return (Instance) treeEntry.getValueOfField("parent");
    }

    private Instance getReference(Instance instance, String definingClass, String fieldName) {
        if (instance == null) return null;
        List<Value> refs = instance.getReferences();
        for (Value ref : refs) {
            if (ref instanceof ObjectFieldValue) {
               ObjectFieldValue fval = (ObjectFieldValue) ref;
               Instance parent = fval.getDefiningInstance();

               if (fval.getField().getName().equals(fieldName) && isSubClassOf(parent, definingClass)) {
                   return parent;
               }
            }
        }
        return null;
    }

    private void addAttribute(Instance dynObjInstance, List<FieldValue> robjRefs) {
        if (DynamicObject.isDynamicObject(dynObjInstance)) {
            List<Value> refs = dynObjInstance.getReferences();

            for (Value ref : refs) {
                Instance defInstance = ref.getDefiningInstance();

                if (PythonObject.isPythonObject(defInstance)) {
                    PythonObject pobject = new PythonObject(defInstance);

                    if (pobject.storage.equals(dynObjInstance)) {
                        for (FieldValue fv : pobject.getAttributes()) {
                            if (fv instanceof ObjectFieldValue) {
                                ObjectFieldValue ofv = (ObjectFieldValue) fv;

                                if (ofv.getInstance().equals(instance)) {
                                    robjRefs.add(fv);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private List getValues() {
        Instance vals = null;

        if (store != null) {
            vals = (Instance)store.getValueOfField("values");
        }
        if (array != null) {
            vals = array;
        }
        if (vals != null) {
            listType = vals.getJavaClass().getName().replace("[]", "");
            if (vals instanceof ObjectArrayInstance) {
                return ((ObjectArrayInstance)vals).getValues();
            }
            if (vals instanceof PrimitiveArrayInstance) {
                isPrimitiveList = true;
                return ((PrimitiveArrayInstance)vals).getValues();
            }
        }
        return Collections.emptyList();
    }

    private int getLength() {
        if (store != null) {
            Integer len = (Integer) store.getValueOfField("length");

            if (len != null) {
                return len.intValue();
            }
            return 0;
        }
        if (array != null) {
            return array.getLength();
        }
        return 0;
    }

    private static boolean isSubClassOf(Instance i, String superClassName) {
        if (i != null) {
            JavaClass superCls = i.getJavaClass();

            for (; superCls != null; superCls = superCls.getSuperClass()) {
                if (superCls.getName().equals(superClassName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<FieldValue> getListFields() {
        return new LazyFieldValues(getValues());
    }

    private List<FieldValue> getMapFields() {
        return getEntriesFromTreeMap(false, map);
    }

    private List<FieldValue> getDictFields() {
        Instance fastStore = (Instance) dictStorage.getValueOfField("store");

        if (DynamicObject.isDynamicObject(fastStore)) {
            return new DynamicObject(fastStore).getFieldValues();
        }
        Instance keywords = (Instance) dictStorage.getValueOfField("keywords");
        if (keywords instanceof ObjectArrayInstance) {
            return getEntriesFromKeywords((ObjectArrayInstance) keywords);
        }
        return getEntriesFromEconomicMapStorage(false, dictStorage);
    }

    private List<FieldValue> getSetFields() {
        Instance m = (Instance) set.getValueOfField("m");
        if (m != null) {    // TreeMap
            return getEntriesFromTreeMap(true, m);
        }
        // EconomicMapStorage
        return getEntriesFromEconomicMapStorage(true, set);
    }

    private List<FieldValue> getEntriesFromKeywords(ObjectArrayInstance keywords) {
        List fields = new ArrayList();

        for (Object keyword : keywords.getValues()) {
            if (keyword instanceof Instance) {
                fields.add(new PythonKeywordEntryFieldValue(false, (Instance) keyword));
            }
        }
        return fields;
    }

    private List<FieldValue> getEntriesFromEconomicMapStorage(boolean isSet, Instance economicMapStorage) {
        List fields = new ArrayList();
        Instance entries = (Instance) economicMapStorage.getValueOfField("entries");

        if (entries instanceof ObjectArrayInstance) {
            ObjectArrayInstance entriesArr = (ObjectArrayInstance) entries;
            String mapClassName = economicMapStorage.getJavaClass().getName();
            int size = entriesArr.getLength();
            List entriesList = entriesArr.getValues();

            for (int i = 0; i < size; i+=2) {
                Instance key = (Instance) entriesList.get(i);
                Instance value = (Instance) entriesList.get(i+1);
                if (key != null) {
                    if (isSet) {
                        fields.add(new PythonEconomicEntryFieldValue(key));
                    } else {    // Map
                        if (value != null) {
                            if (value instanceof Instance) {
                                Instance ival = ((Instance)value);
                                Instance linkValue = (Instance) ival.getValueOfField("value");

                                if (linkValue != null && ival.getJavaClass().getName().startsWith(mapClassName)) {
                                    value = linkValue;
                                }
                            }
                        }
                        fields.add(new PythonEconomicEntryFieldValue(key, value));
                    }
                }
            }
        }
        return fields;
    }

    private List<FieldValue> getEntriesFromTreeMap(boolean isSet, Instance treeMap) {
        List fields = new ArrayList();
        Instance rootEntry = (Instance) treeMap.getValueOfField("root");

        getEntries(isSet, rootEntry, fields);
        return fields;
    }

    private void getEntries(boolean isSet, Instance entry, List fields) {
        if (entry != null) {
            getEntries(isSet, (Instance) entry.getValueOfField("left"), fields);
            fields.add(new PythonMapEntryFieldValue(isSet, entry));
            getEntries(isSet, (Instance) entry.getValueOfField("right"), fields);
        }
    }

    private class PythonEconomicEntryFieldValue extends AbstractPythonMapEntryFieldValue {
        Instance key;
        Instance value;

        private PythonEconomicEntryFieldValue(Instance k) {
            super(true);
            key = k;
        }

        private PythonEconomicEntryFieldValue(Instance k, Instance v) {
            super(false);
            key = k;
            value = v;
        }

        @Override
        Instance getEntryKey() {
            return key;
        }

        @Override
        Instance getEntryValue() {
            return value;
        }
    }

    private class PythonKeywordEntryFieldValue extends AbstractPythonMapEntryFieldValue {
        Instance keyword;

        private PythonKeywordEntryFieldValue(boolean set, Instance k) {
            super(set);
            keyword = k;
        }

        @Override
        Instance getEntryKey() {
            return (Instance) keyword.getValueOfField("name");  // NOI18N
        }

        @Override
        Instance getEntryValue() {
            return (Instance) keyword.getValueOfField("value");  // NOI18N
        }
    }

    private class PythonMapEntryFieldValue extends AbstractPythonMapEntryFieldValue {
        Instance entry;

        private PythonMapEntryFieldValue(boolean set, Instance e) {
            super(set);
            entry = e;
        }

        @Override
        Instance getEntryKey() {
            return (Instance) entry.getValueOfField("key");  // NOI18N
        }

        @Override
        Instance getEntryValue() {
            return (Instance) entry.getValueOfField("value");  // NOI18N
        }
    }

    private abstract class AbstractPythonMapEntryFieldValue implements ObjectFieldValue {

        boolean isSet;

        private AbstractPythonMapEntryFieldValue(boolean set) {
            isSet = set;
        }

        abstract Instance getEntryKey();
        abstract Instance getEntryValue();

        @Override
        public Instance getInstance() {
            if (isSet) {
                return getEntryKey();
            }
            return getEntryValue();
        }

        @Override
        public Field getField() {
            if (isSet) {
                return new PythonMapEntryField("item");
            }
            Instance key = getEntryKey();
            String name = DetailsUtils.getInstanceString(key, null);
            return new PythonMapEntryField(name);
        }

        @Override
        public String getValue() {
            return String.valueOf(getInstance().getInstanceId());
        }

        @Override
        public Instance getDefiningInstance() {
            return instance;
        }

    }

    private class PythonMapEntryField extends PythonField {

        String name;

        private PythonMapEntryField(String n) {
            super(0);
            name = n;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private class PythonFieldValue implements FieldValue {
        private int index;
        Object value;

        private PythonFieldValue(int i, Object val) {
            index = i;
            value = val;
        }

        @Override
        public Field getField() {
            return new PythonField(index);
        }

        @Override
        public String getValue() {
            return (String)value;
        }

        @Override
        public Instance getDefiningInstance() {
            return instance;
        }

    }

    private class PythonObjectFieldValue extends PythonFieldValue implements ObjectFieldValue {

        private PythonObjectFieldValue(int i, Instance val) {
            super(i,val);
        }

        @Override
        public String getValue() {
            return String.valueOf(getInstance().getInstanceId());
        }

        @Override
        public Instance getInstance() {
            return (Instance)value;
        }
    }

    private class PythonField implements Field {

        private int index;

        private PythonField(int i) {
            index = i;
        }

        @Override
        public JavaClass getDeclaringClass() {
            return instance.getJavaClass();
        }

        @Override
        public String getName() {
            return  "["+index+"]";
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public Type getType() {
            return new Type() {
                @Override
                public String getName() {
                    return listType;
                }
            };
        }
    }

    private class LazyFieldValues extends AbstractList<FieldValue> {

        List values;

        private LazyFieldValues(List vals) {
            values = vals;
        }

        @Override
        public FieldValue get(int index) {
            if (isPrimitiveList) {
                return new PythonFieldValue(index, values.get(index));
            }
            return new PythonObjectFieldValue(index, (Instance) values.get(index));
        }

        @Override
        public int size() {
            return getLength();
        }
    }
}