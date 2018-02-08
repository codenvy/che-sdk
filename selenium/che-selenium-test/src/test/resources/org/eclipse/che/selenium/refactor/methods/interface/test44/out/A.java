/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package renameMethodsInInterface.test44;
interface I {
    void k();
}
interface J{
    void k();
}
interface J2 extends J{
    void k();
}

class A{
    public void k(){};
}
class C extends A implements I, J{
    public void k(){};
}
class Test{
    void k(){

    }
}
