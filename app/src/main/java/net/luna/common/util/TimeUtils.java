package net.luna.common.util;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TimeUtils
 * 
 */
@SuppressLint("SimpleDateFormat")
public class TimeUtils {

	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat DATE_FORMAT_DATE = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * 1秒的毫秒数:1000毫秒
	 */
	public static final long oneSecond_ms = 1000;

	/**
	 * 1分钟的毫秒数:60秒*1000毫秒
	 */
	public static final long oneMinute_ms = 60 * oneSecond_ms;

	/**
	 * 1小时的毫秒数:60分钟*60秒*1000毫秒
	 */
	public static final long oneHour_ms = 60 * oneMinute_ms;

	/**
	 * 1天的毫秒数:24小时*60分钟*60秒*1000毫秒
	 */
	public static final long oneDay_ms = 24 * oneHour_ms;

	/**
	 * 1周的毫秒数:7天*24小时*60分钟*60秒*1000毫秒
	 */
	public static final long oneWeek_ms = 7 * oneDay_ms;
	/**
	 * 1个月的毫秒数:31天*24小时*60分钟*60秒*1000毫秒
	 */
	public static final long oneMonth_ms = 31 * oneDay_ms;

	private TimeUtils() {
		throw new AssertionError();
	}

	/**
	 * long time to string
	 * 
	 * @param timeInMillis
	 * @param dateFormat
	 * @return
	 */
	public static String getTime(long timeInMillis, SimpleDateFormat dateFormat) {
		return dateFormat.format(new Date(timeInMillis));
	}

	/**
	 * long time to string, format is {@link #DEFAULT_DATE_FORMAT}
	 * 
	 * @param timeInMillis
	 * @return
	 */
	public static String getTime(long timeInMillis) {
		return getTime(timeInMillis, DEFAULT_DATE_FORMAT);
	}

	/**
	 * get current time in milliseconds
	 * 
	 * @return
	 */
	public static long getCurrentTimeInLong() {
		return System.currentTimeMillis();
	}

	/**
	 * get current time in milliseconds, format is {@link #DEFAULT_DATE_FORMAT}
	 * 
	 * @return
	 */
	public static String getCurrentTimeInString() {
		return getTime(getCurrentTimeInLong());
	}

	/**
	 * get current time in milliseconds
	 * 
	 * @return
	 */
	public static String getCurrentTimeInString(SimpleDateFormat dateFormat) {
		return getTime(getCurrentTimeInLong(), dateFormat);
	}

}
