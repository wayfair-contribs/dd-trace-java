package io.opentracing.contrib.agent.helper;

import com.mongodb.MongoClientOptions;
import io.opentracing.contrib.mongo.TracingCommandListener;
import org.jboss.byteman.rule.Rule;


public class MongoHelperAgent extends DDAgentTracingHelper<MongoClientOptions.Builder> {


	public MongoHelperAgent(Rule rule) {
		super(rule);
	}


	@Override
	protected MongoClientOptions.Builder doPatch(MongoClientOptions.Builder builder) throws Exception {


		TracingCommandListener listener = new TracingCommandListener(tracer);
		builder.addCommandListener(listener);

		return builder;

	}
}
