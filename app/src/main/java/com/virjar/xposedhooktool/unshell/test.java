package com.virjar.xposedhooktool.unshell;

import com.virjar.baksmalisrc.dexlib2.DexFileFactory;
import com.virjar.baksmalisrc.dexlib2.Opcodes;
import com.virjar.baksmalisrc.dexlib2.iface.DexFile;
import com.virjar.baksmalisrc.dexlib2.iface.Method;
import com.virjar.baksmalisrc.dexlib2.rewriter.DexRewriter;
import com.virjar.baksmalisrc.dexlib2.rewriter.Rewriter;
import com.virjar.baksmalisrc.dexlib2.rewriter.RewriterModule;
import com.virjar.baksmalisrc.dexlib2.rewriter.Rewriters;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

public class test {
    private static void testRewrite(String dexfilepath){
        DexFile dexFile;
        try {
            //把要修改的dex load进来
            dexFile = DexFileFactory.loadDexFile(dexfilepath, Opcodes.getDefault());
            //println "dexFile: " + dexFile.getClass().getName()
            DexRewriter rewriter = new DexRewriter(new RewriterModule() {
                @Override
                public Rewriter<Method> getMethodRewriter(
                        @Nonnull Rewriters rewriters) {
                    return new MyMethod();
                }
            });
            //删除原dex
            DexFile rewrittenDexFile = rewriter.rewriteDexFile(dexFile);
            File olddex = new File(dexfilepath);
            if(olddex.exists()){
                //println "delete original dex"
                olddex.delete();
            }
            //生成新dex
            DexFileFactory.writeDexFile(dexfilepath, rewrittenDexFile);
        } catch (IOException e) {
            //println "failed"
            e.printStackTrace();
        }
    }
}

