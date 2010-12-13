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

import com.google.common.base.Functions;
import com.google.common.collect.Ordering;
import com.sun.tools.hat.internal.model.*;
import java.util.*;

/**
 * References by type summary
 *
 */
public class RefsByTypeQuery extends QueryHandler {
    public void run() {
        JavaClass clazz = snapshot.findClass(query);
        if (clazz == null) {
            error("class not found: " + query);
        } else {
            Map<JavaClass, Long> referrersStat = new HashMap<JavaClass, Long>();
            final Map<JavaClass, Long> refereesStat = new HashMap<JavaClass, Long>();
            for (JavaHeapObject instance : clazz.getInstances(false)) {
                if (instance.getId() == -1) {
                    continue;
                }
                for (JavaHeapObject ref : instance.getReferers()) {
                    JavaClass cl = ref.getClazz();
                    if (cl == null) {
                         System.out.println("null class for " + ref);
                         continue;
                    }
                    Long count = referrersStat.get(cl);
                    if (count == null) {
                        count = 1L;
                    } else {
                        ++count;
                    }
                    referrersStat.put(cl, count);
                }
                instance.visitReferencedObjects(
                    new AbstractJavaHeapObjectVisitor() {
                        public void visit(JavaHeapObject obj) {
                            JavaClass cl = obj.getClazz();
                            Long count = refereesStat.get(cl);
                            if (count == null) {
                                count = 1L;
                            } else {
                                ++count;
                            }
                            refereesStat.put(cl, count);
                        }
                    }
                );
            } // for each instance

            startHtml("References by Type");
            out.println("<p align='center'>");
            printClass(clazz);
            if (clazz.getId() != -1) {
                out.println("[" + clazz.getIdString() + "]");
            }
            out.println("</p>");

            if (referrersStat.size() != 0) {
                out.println("<h3 align='center'>Referrers by Type</h3>");
                print(referrersStat);
            }

            if (refereesStat.size() != 0) {
                out.println("<h3 align='center'>Referees by Type</h3>");
                print(refereesStat);
            }

            endHtml();
        }  // clazz != null
    } // run

    private void print(Map<JavaClass, Long> map) {
        out.println("<table border='1' align='center'>");
        JavaClass[] classes = map.keySet().toArray(new JavaClass[map.size()]);
        Arrays.sort(classes, Ordering.natural().onResultOf(Functions.forMap(map)).reverse());

        out.println("<tr><th>Class</th><th>Count</th></tr>");
        for (JavaClass clazz : classes) {
            out.println("<tr><td>");
            out.print("<a href='/refsByType/");
            out.print(clazz.getIdString());
            out.print("'>");
            out.print(clazz.getName());
            out.println("</a>");
            out.println("</td><td>");
            out.println(map.get(clazz));
            out.println("</td></tr>");
        }
        out.println("</table>");
    }
}
