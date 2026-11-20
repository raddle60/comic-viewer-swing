/**
 * 
 */
package com.raddle.comic;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.slf4j.LoggerFactory;

/**
 * @author raddle
 * 
 */
public class ViewConfigHelper {
	private static XmlConfig config = new XmlConfig();
	private static File configFile = new File(System.getProperty("user.home") + "/.comic-view/view-config.xml");
	static {
		if (!configFile.getParentFile().exists()) {
			configFile.mkdirs();
		}
		if (!configFile.exists()) {
			save();
		} else {
			try {
				config.load(new InputStreamReader(new FileInputStream(configFile), "utf-8"));
			} catch (Exception e) {
				LoggerFactory.getLogger(ViewConfigHelper.class).error(e.getMessage(), e);
			}
		}
	}

	private static void save() {
		try {
			config.saveTo(configFile);
		} catch (Exception e) {
			LoggerFactory.getLogger(ViewConfigHelper.class).error(e.getMessage(), e);
		}
	}

	public static void setContinueView(boolean isContinueView) {
		config.setProperty("view.isContinueView", isContinueView + "");
		save();
	}

	public static boolean getIsContinueView() {
		return config.getBoolean("view.isContinueView", false);
	}

	public static void setSeamlessView(boolean isSeamlessView) {
		config.setProperty("view.isSeamlessView", isSeamlessView + "");
		save();
	}

	public static boolean getIsSeamlessView() {
		return config.getBoolean("view.isSeamlessView", false);
	}

	public static void setContinuousDownload(boolean isContinuousDownload) {
		config.setProperty("view.isContinuousDownload", isContinuousDownload + "");
		save();
	}

	public static boolean getIsContinuousDownload() {
		return config.getBoolean("view.isContinuousDownload", false);
	}

	public static void setZoomPercent(int zoomPercent) {
		config.setProperty("view.zoomPercent", zoomPercent + "");
		save();
	}

	public static int getZoomPercent() {
		return config.getInt("view.zoomPercent", 100);
	}

	public static void setFitHeigthView(boolean isFitHeigthView) {
		config.setProperty("view.isFitHeigthView", isFitHeigthView + "");
		save();
	}

	public static boolean getIsFitHeigthView() {
		return config.getBoolean("view.isFitHeigthView", false);
	}

}
