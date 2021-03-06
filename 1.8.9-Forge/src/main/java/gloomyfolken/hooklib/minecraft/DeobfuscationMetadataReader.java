package gloomyfolken.hooklib.minecraft;

import gloomyfolken.hooklib.asm.ClassMetadataReader;
import java.io.IOException;
import java.lang.reflect.Method;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class DeobfuscationMetadataReader extends ClassMetadataReader {
   private static Method runTransformers;

   public byte[] getClassData(String className) throws IOException {
      byte[] bytes = super.getClassData(unmap(className.replace('.', '/')));
      return deobfuscateClass(className, bytes);
   }

   protected boolean checkSameMethod(String sourceName, String sourceDesc, String targetName, String targetDesc) {
      return checkSameMethod(sourceName, targetName) && sourceDesc.equals(targetDesc);
   }

   protected ClassMetadataReader.MethodReference getMethodReferenceASM(String type, String methodName, String desc) throws IOException {
      ClassMetadataReader.FindMethodClassVisitor cv = new ClassMetadataReader.FindMethodClassVisitor(methodName, desc);
      byte[] bytes = getTransformedBytes(type);
      this.acceptVisitor(bytes, cv);
      return cv.found ? new ClassMetadataReader.MethodReference(type, cv.targetName, cv.targetDesc) : null;
   }

   static byte[] deobfuscateClass(String className, byte[] bytes) {
      if (HookLoader.getDeobfuscationTransformer() != null) {
         bytes = HookLoader.getDeobfuscationTransformer().transform(className, className, bytes);
      }

      return bytes;
   }

   private static byte[] getTransformedBytes(String type) throws IOException {
      String obfName = unmap(type);
      byte[] bytes = Launch.classLoader.getClassBytes(obfName);
      if (bytes == null) {
         throw new RuntimeException("Bytes for " + obfName + " not found");
      } else {
         try {
            bytes = (byte[])((byte[])runTransformers.invoke(Launch.classLoader, obfName, type, bytes));
         } catch (Exception var4) {
            var4.printStackTrace();
         }

         return bytes;
      }
   }

   private static String unmap(String type) {
      return HookLibPlugin.getObfuscated() ? FMLDeobfuscatingRemapper.INSTANCE.unmap(type) : type;
   }

   private static boolean checkSameMethod(String srgName, String mcpName) {
      if (HookLibPlugin.getObfuscated() && MinecraftClassTransformer.instance != null) {
         int methodId = MinecraftClassTransformer.getMethodId(srgName);
         String remappedName = (String)MinecraftClassTransformer.instance.getMethodNames().get(methodId);
         if (remappedName != null && remappedName.equals(mcpName)) {
            return true;
         }
      }

      return srgName.equals(mcpName);
   }

   static {
      try {
         runTransformers = LaunchClassLoader.class.getDeclaredMethod("runTransformers", String.class, String.class, byte[].class);
         runTransformers.setAccessible(true);
      } catch (Exception var1) {
         var1.printStackTrace();
      }

   }
}
