package com.virjar.xposedhooktool.unshell;

import android.support.annotation.Nullable;

import com.virjar.baksmalisrc.dexlib2.dexbacked.DexBackedMethod;
import com.virjar.baksmalisrc.dexlib2.dexbacked.DexBackedMethodImplementation;
import com.virjar.baksmalisrc.dexlib2.iface.Annotation;
import com.virjar.baksmalisrc.dexlib2.iface.Method;
import com.virjar.baksmalisrc.dexlib2.iface.MethodImplementation;
import com.virjar.baksmalisrc.dexlib2.iface.MethodParameter;
import com.virjar.baksmalisrc.dexlib2.iface.reference.MethodReference;
import com.virjar.baksmalisrc.dexlib2.rewriter.MethodRewriter;
import com.virjar.baksmalisrc.dexlib2.rewriter.Rewriter;
import com.virjar.baksmalisrc.dexlib2.util.ReferenceUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

public class MyMethod implements Rewriter<Method> {

    @Nonnull
    @Override
    public Method rewrite(@Nonnull final Method value) {
        //找到 helloMethod
        if (value.getName().contains("helloMethod")) {
            //println "rewrite: "+value.getName();
            return new Method() {
                @Override
                public int compareTo(@Nonnull MethodReference o) {
                    return value.compareTo(o);
                }
                @Nonnull
                @Override
                public List<? extends CharSequence> getParameterTypes() {
                    return value.getParameters();
                }
                @Nonnull
                @Override
                public String getDefiningClass() {
                    return value.getDefiningClass();
                }
                @Nonnull
                @Override
                public List<? extends MethodParameter> getParameters() {
                    return value.getParameters();
                }
                @Nonnull
                @Override
                public String getReturnType() {
                    return value.getReturnType();
                }
                @Override
                public int getAccessFlags() {
                    return value.getAccessFlags();
                }
                @javax.annotation.Nullable
                @Override
                public MethodImplementation getImplementation() {
                    return value.getImplementation();
                }
                @Nonnull
                @Override
                public Set<? extends Annotation> getAnnotations() {
                    return value.getAnnotations();
                }
                @Nonnull
                @Override
                //将helloMethod重命名为MyMethod
                public String getName() {
                    return "MyMethod";
                }
            };
        }
        return value;
    }

}
