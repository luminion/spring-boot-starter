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

package io.github.luminion.starter.support.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
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
public class JacksonXssDeserializer extends JsonDeserializer<String> {
	/**
	 * 清理方法
	 */
	private final Function<String,String> func;

	/**
	 * 反序列化方法，用于处理JSON字符串的反序列化并进行XSS清洗
	 * @param p JSON解析器
	 * @param ctx 反序列化上下文
	 * @return 经过XSS清洗后的字符串，如果输入为null则返回null
	 * @throws IOException 当反序列化过程中发生I/O错误时抛出
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
		// xss 配置
//		return this.clean(p.getParsingContext().getCurrentName(), text);
		return func.apply(text);
	}


//	/**
//	 * 清理文本内容，根据XSS防护设置进行处理
//	 * @param name 属性名称
//	 * @param text 待清理的文本
//	 * @return 清理后的文本
//	 * @throws IOException 如果清理过程中发生IO异常
//	 */
//	@Override
//	public String clean(String name, String text) throws IOException {
//		if (XssHolder.isEnabled() && Objects.isNull(XssHolder.getXssCleanIgnore())) {
//			String value = xssCleaner.clean(XssUtil.trim(text, properties.isTrimText()));
//			log.debug("Json property value:{} cleaned up by mica-xss, current value is:{}.", text, value);
//			return value;
//		}
//		else if (XssHolder.isEnabled() && Objects.nonNull(XssHolder.getXssCleanIgnore())) {
//			XssCleanIgnore xssCleanIgnore = XssHolder.getXssCleanIgnore();
//			if (ArrayUtil.contains(xssCleanIgnore.value(), name)) {
//				return XssUtil.trim(text, properties.isTrimText());
//			}
//
//			String value = xssCleaner.clean(XssUtil.trim(text, properties.isTrimText()));
//			log.debug("Json property value:{} cleaned up by mica-xss, current value is:{}.", text, value);
//			return value;
//		}
//		else {
//			return XssUtil.trim(text, properties.isTrimText());
//		}
//	}
}
