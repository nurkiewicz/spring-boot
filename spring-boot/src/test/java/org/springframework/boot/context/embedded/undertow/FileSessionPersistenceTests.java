/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.undertow;

import io.undertow.servlet.api.SessionPersistenceManager.PersistentSession;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FileSessionPersistence}.
 *
 * @author Phillip Webb
 */
public class FileSessionPersistenceTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private File folder;

	private FileSessionPersistence persistence;

	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	private Date expiration = new Date(System.currentTimeMillis() + 10000);

	@Before
	public void setup() throws IOException {
		this.folder = this.temp.newFolder();
		this.persistence = new FileSessionPersistence(this.folder);
	}

	@Test
	public void loadsNullForMissingFile() throws Exception {
		Map<String, PersistentSession> attributes = this.persistence
				.loadSessionAttributes("test", this.classLoader);
		assertThat(attributes, nullValue());
	}

	@Test
	public void presistAndLoad() throws Exception {
		Map<String, PersistentSession> sessionData = new LinkedHashMap<String, PersistentSession>();
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("spring", "boot");
		PersistentSession session = new PersistentSession(this.expiration, data);
		sessionData.put("abc", session);
		this.persistence.persistSessions("test", sessionData);
		Map<String, PersistentSession> restored = this.persistence.loadSessionAttributes(
				"test", this.classLoader);
		assertThat(restored, notNullValue());
		assertThat(restored.get("abc").getExpiration(), equalTo(this.expiration));
		assertThat(restored.get("abc").getSessionData().get("spring"),
				equalTo((Object) "boot"));
	}

	@Test
	public void dontRestoreExpired() throws Exception {
		Date expired = new Date(System.currentTimeMillis() - 1000);
		Map<String, PersistentSession> sessionData = new LinkedHashMap<String, PersistentSession>();
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("spring", "boot");
		PersistentSession session = new PersistentSession(expired, data);
		sessionData.put("abc", session);
		this.persistence.persistSessions("test", sessionData);
		Map<String, PersistentSession> restored = this.persistence.loadSessionAttributes(
				"test", this.classLoader);
		assertThat(restored, notNullValue());
		assertThat(restored.containsKey("abc"), equalTo(false));
	}

	@Test
	public void deleteFileOnClear() throws Exception {
		File sessionFile = new File(this.folder, "test.session");
		Map<String, PersistentSession> sessionData = new LinkedHashMap<String, PersistentSession>();
		this.persistence.persistSessions("test", sessionData);
		assertThat(sessionFile.exists(), equalTo(true));
		this.persistence.clear("test");
		assertThat(sessionFile.exists(), equalTo(false));
	}

}
