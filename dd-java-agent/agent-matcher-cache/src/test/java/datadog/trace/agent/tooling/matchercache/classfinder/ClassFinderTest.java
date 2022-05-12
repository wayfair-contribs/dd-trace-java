package datadog.trace.agent.tooling.matchercache.classfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

public class ClassFinderTest {
  public final String TEST_CLASSES_FOLDER =
      this.getClass().getClassLoader().getResource("test-classes").getFile();
  public final String TEST_CLASSES_FOLDER_JAVA_11 =
      this.getClass().getClassLoader().getResource("test-classes-11").getFile();

  public static void assertClasses(Set<String> expectedClasses, Set<ClassData> actualClassData) {
    Set<String> actualClasses = new HashSet<>();
    for (ClassData cd : actualClassData) {
      actualClasses.add(cd.getFullClassName());
    }
    assertEquals(expectedClasses, actualClasses);
  }

  private final ClassFinder classFinder = new ClassFinder();

  @Test
  void testInnerJars() throws IOException {
    ClassCollection classCollection =
        classFinder.findClassesIn(new File(TEST_CLASSES_FOLDER, "inner-jars"));

    Set<String> expectedClasses = new HashSet<>();
    expectedClasses.add("example.InnerJarClass");
    expectedClasses.add("example.MiddleJarClass");
    expectedClasses.add("example.OuterJarClass");

    assertClasses(expectedClasses, classCollection.allClasses(7));
    assertClasses(expectedClasses, classCollection.allClasses(8));
    assertClasses(expectedClasses, classCollection.allClasses(9));
    assertClasses(expectedClasses, classCollection.allClasses(11));
  }

  @Test
  void testMultiReleaseClasses() throws IOException {
    ClassCollection classCollection =
        classFinder.findClassesIn(new File(TEST_CLASSES_FOLDER, "multi-release-jar"));

    Set<String> expectedClasses = new HashSet<>();
    expectedClasses.add("example.classes.Abc");

    assertClasses(expectedClasses, classCollection.allClasses(7));
    assertClasses(expectedClasses, classCollection.allClasses(8));

    expectedClasses.add("example.classes.Only9");

    assertClasses(expectedClasses, classCollection.allClasses(9));
    assertClasses(expectedClasses, classCollection.allClasses(11));
  }

  @Test
  void testRelocatedClasses() throws IOException {
    ClassCollection classCollection =
        classFinder.findClassesIn(new File(TEST_CLASSES_FOLDER, "relocated-classes"));

    Set<String> expectedClasses = new HashSet<>();
    expectedClasses.add("bar.foo.Baz");

    assertClasses(expectedClasses, classCollection.allClasses(7));
    assertClasses(expectedClasses, classCollection.allClasses(8));
    assertClasses(expectedClasses, classCollection.allClasses(9));
    assertClasses(expectedClasses, classCollection.allClasses(11));
  }

  @Test
  void testRenamedClassFile() throws IOException {
    ClassCollection classCollection =
        classFinder.findClassesIn(new File(TEST_CLASSES_FOLDER, "renamed-class-file"));

    Set<String> expectedClasses = new HashSet<>();
    expectedClasses.add("foo.bar.FooBar");

    assertClasses(expectedClasses, classCollection.allClasses(7));
    assertClasses(expectedClasses, classCollection.allClasses(8));
    assertClasses(expectedClasses, classCollection.allClasses(9));
    assertClasses(expectedClasses, classCollection.allClasses(11));
  }

  @Test
  void testStandardLayout() throws IOException {
    ClassCollection classCollection =
        classFinder.findClassesIn(new File(TEST_CLASSES_FOLDER, "standard-layout"));

    Set<String> expectedClasses = new HashSet<>();
    expectedClasses.add("foo.bar.xyz.Xyz");

    assertClasses(expectedClasses, classCollection.allClasses(7));
    assertClasses(expectedClasses, classCollection.allClasses(8));
    assertClasses(expectedClasses, classCollection.allClasses(9));
    assertClasses(expectedClasses, classCollection.allClasses(11));
  }

  @Test
  void testAllTogether() throws IOException {
    ClassCollection classCollection = classFinder.findClassesIn(new File(TEST_CLASSES_FOLDER));

    Set<String> expectedClasses = new HashSet<>();
    expectedClasses.add("example.InnerJarClass");
    expectedClasses.add("foo.bar.xyz.Xyz");
    expectedClasses.add("example.classes.Abc");
    expectedClasses.add("example.MiddleJarClass");
    expectedClasses.add("example.OuterJarClass");
    expectedClasses.add("bar.foo.Baz");
    expectedClasses.add("foo.bar.FooBar");

    assertClasses(expectedClasses, classCollection.allClasses(7));
    assertClasses(expectedClasses, classCollection.allClasses(8));

    expectedClasses.add("example.classes.Only9");
    assertClasses(expectedClasses, classCollection.allClasses(9));
    assertClasses(expectedClasses, classCollection.allClasses(11));
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_11)
  void testJavaModule() throws IOException {
    ClassCollection classCollection =
        classFinder.findClassesIn(new File(TEST_CLASSES_FOLDER_JAVA_11, "java-module"));

    Set<String> expectedClasses = new HashSet<>();
    expectedClasses.add("org.company.Abc");

    assertClasses(expectedClasses, classCollection.allClasses(7));
    assertClasses(expectedClasses, classCollection.allClasses(8));
    assertClasses(expectedClasses, classCollection.allClasses(9));
    assertClasses(expectedClasses, classCollection.allClasses(11));
  }
}