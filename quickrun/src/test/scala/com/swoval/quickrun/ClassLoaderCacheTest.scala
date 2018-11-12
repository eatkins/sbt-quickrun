package com.swoval.quickrun

import java.nio.file.attribute.FileTime
import java.nio.file.{ Files, Paths, StandardCopyOption }

import utest._
import TestHelpers._
import com.swoval.reflect.ClassLoaders

object ClassLoaderCacheTest extends TestSuite {
  def withCache[R](size: Int)(f: ClassLoaderCache => R): R = {
    val cache = new ClassLoaderCache(size)
    try f(cache)
    finally cache.close()
  }
  val tests: Tests = Tests {
    'get - {
      'size - {
        'space - withCache(0) { cache =>
          val classPath = JarClassPath(Nil)
          val firstLoader = cache.get(classPath)
          val secondLoader = cache.get(classPath)
          assert(firstLoader != secondLoader)
        }
        'noSpace - withCache(1) { cache =>
          val classPath = JarClassPath(Nil)
          val firstLoader = cache.get(classPath)
          val secondLoader = cache.get(classPath)
          assert(firstLoader == secondLoader)
        }
        'eviction - withCache(2) { cache =>
          val cache = new ClassLoaderCache(2)
          val firstClassPath = JarClassPath(Nil)
          val secondClassPath = JarClassPath(Paths.get("") :: Nil)
          val thirdClassPath = JarClassPath(Paths.get("") :: Nil, (Paths.get("") -> 0L) :: Nil)
          val firstLoader = cache.get(firstClassPath)
          val secondLoader = cache.get(secondClassPath)
          val thirdLoader = cache.get(thirdClassPath)
          assert(cache.get(thirdClassPath) == thirdLoader)
          assert(cache.get(secondClassPath) == secondLoader)
          assert(cache.get(firstClassPath) != firstLoader)
          assert(cache.get(thirdClassPath) != thirdLoader)
        }
      }
      'snapshot - {
        'invalidate - withCache(1) { cache =>
          withTempDirectory { dir =>
            val initialJar = resourceDir.resolve("1").resolve("quickrun-test.jar")
            val targetJar = dir.resolve("quickrun-test-SNAPSHOT.jar")
            Files.copy(initialJar, targetJar)
            def jarClassPath() =
              JarClassPath(Nil, targetJar -> Files.getLastModifiedTime(targetJar).toMillis :: Nil)
            val initialJarClassPath = jarClassPath()
            val initLoader = cache.get(initialJarClassPath)
            def invoke(loader: ClassLoader): AnyRef =
              ClassLoaders.invokeStaticMethod(loader, "com.swoval.quickrun.TestModule", "static")
            invoke(initLoader) ==> 1

            val secondJar = resourceDir.resolve("2").resolve("quickrun-test.jar")

            Files.copy(secondJar, targetJar, StandardCopyOption.REPLACE_EXISTING)
            Files.setLastModifiedTime(
              targetJar,
              FileTime.fromMillis(Files.getLastModifiedTime(targetJar).toMillis + 2000))
            val secondJarClassPath = jarClassPath()
            val secondLoader = cache.get(secondJarClassPath)
            assert(initLoader != secondLoader)
            invoke(secondLoader) ==> 2
            assert(cache.get(secondJarClassPath) == secondLoader)
            assert(cache.get(initialJarClassPath) != initLoader)
          }
        }
      }
    }
  }
}
