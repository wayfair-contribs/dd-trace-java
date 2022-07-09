package datadog.trace.agent.tooling.bytebuddy.matcher;

import java.security.ProtectionDomain;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;

public final class RawInstrumenterMatcher implements AgentBuilder.RawMatcher {
  private final InstrumenterMatcher instrumenterMatcher;

  public RawInstrumenterMatcher(int instrumentationId) {
    instrumenterMatcher = new InstrumenterMatcher(instrumentationId);
  }

  @Override
  public boolean matches(
      TypeDescription typeDescription,
      ClassLoader classLoader,
      JavaModule module,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain) {
    return instrumenterMatcher.doMatch(typeDescription);
  }
}
