package com.raddle.comic.engine;

import java.io.File;

public class ChannelInfo {
	private String name;
	private String home;
	private String desc;
	private File scriptFile;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public File getScriptFile() {
		return scriptFile;
	}

	public void setScriptFile(File scriptFile) {
		this.scriptFile = scriptFile;
	}

	@Override
	public String toString() {
		return name;
	}
}
