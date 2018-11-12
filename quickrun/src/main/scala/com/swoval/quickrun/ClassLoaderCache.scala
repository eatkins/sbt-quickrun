package com.swoval.quickrun

import java.util.concurrent.{ ConcurrentHashMap, LinkedBlockingDeque }

import com.swoval.reflect.ChildFirstClassLoader

class ClassLoaderCache(val maxSize: Int) extends AutoCloseable {
  private[this] val classLoaders = new ConcurrentHashMap[JarClassPath, ClassLoader]
  private[this] val recentlyUsed = new LinkedBlockingDeque[JarClassPath]()
  def get(jarClassPath: JarClassPath): ClassLoader = recentlyUsed.synchronized {
    classLoaders.get(jarClassPath) match {
      case null =>
        val loader = new ChildFirstClassLoader(jarClassPath.urls)
        if (recentlyUsed.size >= maxSize && maxSize > 0) {
          while (recentlyUsed.size >= maxSize) {
            Option(recentlyUsed.poll()).foreach(classLoaders.remove)
          }
        }
        if (recentlyUsed.size < maxSize) {
          recentlyUsed.addLast(jarClassPath)
          classLoaders.put(jarClassPath, loader)
        }
        loader
      case l =>
        recentlyUsed.remove(jarClassPath)
        recentlyUsed.addLast(jarClassPath)
        l
    }
  }
  override def close(): Unit = {
    classLoaders.clear()
    recentlyUsed.clear()
  }
}
