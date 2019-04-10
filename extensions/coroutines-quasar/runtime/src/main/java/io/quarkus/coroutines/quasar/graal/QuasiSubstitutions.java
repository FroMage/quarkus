package io.quarkus.coroutines.quasar.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.github.fromage.quasi.common.util.ExtendedStackTraceClassContext")
final class Target_com_github_fromage_quasi_common_util_ExtendedStackTraceClassContext {
}

@TargetClass(className = "com.github.fromage.quasi.common.util.ExtendedStackTrace")
final class Target_com_github_fromage_quasi_common_util_ExtendedStackTrace {
    @Alias
    Target_com_github_fromage_quasi_common_util_ExtendedStackTrace(Throwable t) {
    }

    @Substitute
    public static Target_com_github_fromage_quasi_common_util_ExtendedStackTrace of(Throwable t) {
        return new Target_com_github_fromage_quasi_common_util_ExtendedStackTrace(t);
    }

    @Substitute
    public static Target_com_github_fromage_quasi_common_util_ExtendedStackTrace here() {
        return (Target_com_github_fromage_quasi_common_util_ExtendedStackTrace) (Object) new Target_com_github_fromage_quasi_common_util_ExtendedStackTraceClassContext();
    }
}

@Delete
@TargetClass(className = "com.github.fromage.quasi.common.util.ExtendedStackTraceHotSpot")
final class Target_com_github_fromage_quasi_common_util_ExtendedStackTraceHotSpot {

}

public class QuasiSubstitutions {

}
