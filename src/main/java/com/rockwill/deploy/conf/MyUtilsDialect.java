package com.rockwill.deploy.conf;

import com.rockwill.deploy.utils.ThymeleafUtils;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.processor.IProcessor;

import java.util.Collections;
import java.util.Set;

public class MyUtilsDialect extends AbstractProcessorDialect implements IExpressionObjectDialect {

    private static final String DIALECT_PREFIX = "myu";
    private final ThymeleafUtils stringNormalizer = new ThymeleafUtils();

    public MyUtilsDialect() {
        super("My Utils Dialect", DIALECT_PREFIX, 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Collections.emptySet();
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Collections.singleton("normalizer");
            }

            @Override
            public Object buildObject(IExpressionContext iExpressionContext, String s) {
                if ("normalizer".equals(s)) {
                    return stringNormalizer;
                }
                return null;
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }
}