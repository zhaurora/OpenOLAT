/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.nodes.cl;

import java.util.Locale;

import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.nodes.AbstractCourseNodeConfiguration;
import org.olat.course.nodes.CheckListCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeConfiguration;

/**
 * 
 * Initial date: 04.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CheckListCourseNodeConfiguration extends AbstractCourseNodeConfiguration implements CourseNodeConfiguration {

	private CheckListCourseNodeConfiguration() {
		super();
	}
	
	@Override
	public CourseNode getInstance() {
		return new CheckListCourseNode();
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getLinkText(java.util.Locale)
	 */
	@Override
	public String getLinkText(Locale locale) {
		Translator fallback = Util.createPackageTranslator(CourseNodeConfiguration.class, locale);
		Translator translator = Util.createPackageTranslator(this.getClass(), locale, fallback);
		return translator.translate("title_checklist");
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getCSSClass()
	 */
	@Override
	public String getIconCSSClass() {
		return "o_cl_icon";
	}

	/**
	 * @see org.olat.course.nodes.CourseNodeConfiguration#getLinkCSSClass()
	 */
	@Override
	public String getLinkCSSClass() {
		return null;
	}

	@Override
	public String getAlias() {
		return "checklist";
	}
}
