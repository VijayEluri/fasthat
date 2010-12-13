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

package com.sun.tools.hat.internal.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * @author      A. Sundararajan
 */

public class ReachableObjects {
    private enum Sorters implements Function<JavaThing, Comparable<?>>,
            Comparator<JavaThing> {
        BY_SIZE {
            @Override
            public Integer apply(JavaThing thing) {
                return thing.getSize();
            }
        };

        private final Ordering<JavaThing> ordering;

        private Sorters() {
            this.ordering = Ordering.natural().onResultOf(this);
        }

        @Override
        public int compare(JavaThing lhs, JavaThing rhs) {
            return ordering.compare(lhs, rhs);
        }
    }

    public ReachableObjects(JavaHeapObject root,
                            final ReachableExcludes excludes) {
        this.root = root;

        final Set<JavaHeapObject> bag = new HashSet<JavaHeapObject>();
        final Set<String> fieldsExcluded = new HashSet<String>();
        final Set<String> fieldsUsed = new HashSet<String>();
        JavaHeapObjectVisitor visitor = new AbstractJavaHeapObjectVisitor() {
            public void visit(JavaHeapObject t) {
                // Size is zero for things like integer fields
                if (t != null && t.getSize() > 0 && !bag.contains(t)) {
                    bag.add(t);
                    t.visitReferencedObjects(this);
                }
            }

            public boolean mightExclude() {
                return excludes != null;
            }

            public boolean exclude(JavaClass clazz, JavaField f) {
                if (excludes == null) {
                    return false;
                }
                String nm = clazz.getName() + "." + f.getName();
                if (excludes.isExcluded(nm)) {
                    fieldsExcluded.add(nm);
                    return true;
                } else {
                    fieldsUsed.add(nm);
                    return false;
                }
            }
        };
        // Put the closure of root and all objects reachable from root into
        // bag (depth first), but don't include root:
        visitor.visit(root);
        bag.remove(root);

        // Now grab the elements into a vector, and sort it in decreasing size
        JavaThing[] things = new JavaThing[bag.size()];
        int i = 0;
        for (JavaHeapObject thing : bag) {
            things[i++] = thing;
        }
        Arrays.sort(things, new Comparator<JavaThing>() {
            public int compare(JavaThing left, JavaThing right) {
                return ComparisonChain.start()
                        .compare(right, left, Sorters.BY_SIZE)
                        .compare(left, right)
                        .result();
            }
        });
        this.reachables = things;

        long totalSize = root.getSize();
        for (JavaThing thing : things) {
            totalSize += thing.getSize();
        }
        this.totalSize = totalSize;

        excludedFields = getElements(fieldsExcluded);
        usedFields = getElements(fieldsUsed);
    }

    public JavaHeapObject getRoot() {
        return root;
    }

    public JavaThing[] getReachables() {
        return reachables;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String[] getExcludedFields() {
        return excludedFields;
    }

    public String[] getUsedFields() {
        return usedFields;
    }

    private static String[] getElements(Set<String> set) {
        String[] res = set.toArray(new String[set.size()]);
        Arrays.sort(res);
        return res;
    }

    private final JavaHeapObject root;
    private final JavaThing[] reachables;
    private final String[]  excludedFields;
    private final String[]  usedFields;
    private final long totalSize;
}
