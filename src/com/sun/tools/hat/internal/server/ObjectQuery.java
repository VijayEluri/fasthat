/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package com.sun.tools.hat.internal.server;

import java.util.Arrays;
import java.util.Comparator;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sun.tools.hat.internal.model.*;

/**
 *
 * @author      Bill Foote
 */


class ObjectQuery extends ClassQuery {
        // We inherit printFullClass from ClassQuery

    private enum Sorters implements Function<FieldThing, Comparable<?>>,
            Comparator<FieldThing> {
        BY_FIELD_NAME {
            @Override
            public String apply(FieldThing ft) {
                return ft.field.getName();
            }
        };

        private final Ordering<FieldThing> ordering;

        private Sorters() {
            this.ordering = Ordering.natural().onResultOf(this);
        }

        @Override
        public int compare(FieldThing lhs, FieldThing rhs) {
            return ordering.compare(lhs, rhs);
        }
    }

    private static class FieldThing {
        public final JavaField field;
        public final JavaThing thing;

        public FieldThing(JavaField field, JavaThing thing) {
            this.field = field;
            this.thing = thing;
        }

        public static FieldThing[] make(JavaField[] fields, JavaThing[] things) {
            int len = Ints.min(fields.length, things.length);
            FieldThing[] result = new FieldThing[len];
            for (int i = 0; i < len; ++i) {
                result[i] = new FieldThing(fields[i], things[i]);
            }
            return result;
        }
    }

    public ObjectQuery() {
    }

    public void run() {
        startHtml("Object at " + query);
        long id = parseHex(query);
        JavaHeapObject thing = snapshot.findThing(id);
        //
        // In the following, I suppose we really should use a visitor
        // pattern.  I'm not that strongly motivated to do this, however:
        // This is the only typecase there is, and the default for an
        // unrecognized type is to do something reasonable.
        //
        if (thing == null) {
            error("object not found");
        } else if (thing instanceof JavaClass) {
            printFullClass((JavaClass) thing);
        } else if (thing instanceof JavaValueArray) {
            print(((JavaValueArray) thing).valueString(true));
            printAllocationSite(thing);
            printReferencesTo(thing);
        } else if (thing instanceof JavaObjectArray) {
            printFullObjectArray((JavaObjectArray) thing);
            printAllocationSite(thing);
            printReferencesTo(thing);
        } else if (thing instanceof JavaObject) {
            printFullObject((JavaObject) thing);
            printAllocationSite(thing);
            printReferencesTo(thing);
        } else {
            // We should never get here
            print(thing.toString());
            printReferencesTo(thing);
        }
        endHtml();
    }


    private void printFullObject(JavaObject obj) {
        out.print("<h1>instance of ");
        print(obj.toString());
        out.print(" <small>(" + obj.getSize() + " bytes)</small>");
        out.println("</h1>\n");

        out.println("<h2>Class:</h2>");
        printClass(obj.getClazz());

        out.println("<h2>Instance data members:</h2>");
        FieldThing[] fieldThings = FieldThing.make(
                obj.getClazz().getFieldsForInstance(), obj.getFields());
        Arrays.sort(fieldThings, Sorters.BY_FIELD_NAME);
        for (FieldThing fieldThing : fieldThings) {
            printField(fieldThing.field);
            out.print(" : ");
            printThing(fieldThing.thing);
            out.println("<br>");
        }
    }

    private void printFullObjectArray(JavaObjectArray arr) {
        JavaThing[] elements = arr.getElements();
        out.println("<h1>Array of " + elements.length + " objects</h1>");

        out.println("<h2>Class:</h2>");
        printClass(arr.getClazz());

        out.println("<h2>Values</h2>");
        for (int i = 0; i < elements.length; i++) {
            out.print("" + i + " : ");
            printThing(elements[i]);
            out.println("<br>");
        }
    }

    //
    // Print the StackTrace where this was allocated
    //
    private void printAllocationSite(JavaHeapObject obj) {
        StackTrace trace = obj.getAllocatedFrom();
        if (trace == null || trace.getFrames().length == 0) {
            return;
        }
        out.println("<h2>Object allocated from:</h2>");
        printStackTrace(trace);
    }
}
