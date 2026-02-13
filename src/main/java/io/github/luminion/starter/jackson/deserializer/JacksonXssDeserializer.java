/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.luminion.starter.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.github.luminion.starter.core.xss.XssCleanIgnore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Function;

/**
 * 杰克逊xss解串器
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class JacksonXssDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {
	/**
	 * 清理方法
	 */
	private final Function<String, String> func;

	/**
	 * 是否忽略清理
	 */
	private final boolean ignore;

	public JacksonXssDeserializer(Function<String, String> func) {
		this(func, false);
	}

	/**
	 * 反序列化方法，用于处理JSON字符串的反序列化并进行XSS清洗
	 * 
	 * @param p   JSON解析器
	 * @param ctx 反序列化上下文
	 * @return 经过XSS清洗后的字符串，如果输入为null则返回null
	 * @throws IOException              当反序列化过程中发生I/O错误时抛出
	 * @throws MismatchedInputException 当当前JSON令牌不是VALUE_STRING时抛出
	 */
	@Override
	public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
		JsonToken jsonToken = p.getCurrentToken();
		if (JsonToken.VALUE_STRING != jsonToken) {
			throw MismatchedInputException.from(p, String.class,
					"can't deserialize value of type java.lang.String from " + jsonToken);
		}
		// 解析字符串
		String text = p.getValueAsString();
		if (text == null) {
			return null;
		}

		// 如果标记了忽略，直接返回原文本
		if (ignore) {
			return text;
		}

		return func.apply(text);
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
		if (property != null) {
			XssCleanIgnore xssCleanIgnore = property.getAnnotation(XssCleanIgnore.class);
			if (xssCleanIgnore != null) {
				return new JacksonXssDeserializer(func, true);
			}
		}
		return this;
	}
}
