
package com.util.log;

import java.io.IOException;
import java.io.File;
import java.io.InterruptedIOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

public class MyDailyRollingFileAppender extends FileAppender {

	// The code assumes that the following constants are in a increasing
	// sequence.
	static final int TOP_OF_TROUBLE = -1;
	static final int TOP_OF_MINUTE = 0;
	static final int TOP_OF_HOUR = 1;
	static final int HALF_DAY = 2;
	static final int TOP_OF_DAY = 3;
	static final int TOP_OF_WEEK = 4;
	static final int TOP_OF_MONTH = 5;

	/**
	 * The date pattern. By default, the pattern is set to "'.'yyyy-MM-dd"
	 * meaning daily rollover.
	 */
	private String datePattern = "'.'yyyy-MM-dd";
	private int MaxSize = 3;

	/**
	 * The log file will be renamed to the value of the scheduledFilename
	 * variable when the next interval is entered. For example, if the rollover
	 * period is one hour, the log file will be renamed to the value of
	 * "scheduledFilename" at the beginning of the next hour.
	 * 
	 * The precise time when a rollover occurs depends on logging activity.
	 */
	private String scheduledFilename;

	/**
	 * The next time we estimate a rollover should occur.
	 */
	private long nextCheck = System.currentTimeMillis() - 1;

	Date now = new Date();

	SimpleDateFormat sdf;

	RollingCalendar rc = new RollingCalendar();

	int checkPeriod = TOP_OF_TROUBLE;

	// The gmtTimeZone is used only in computeCheckPeriod() method.
	static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");

	/**
	 * The default constructor does nothing.
	 */
	public MyDailyRollingFileAppender() {
	}

	/**
	 * Instantiate a <code>DailyRollingFileAppender</code> and open the file
	 * designated by <code>filename</code>. The opened filename will become the
	 * ouput destination for this appender.
	 * 
	 */
	public MyDailyRollingFileAppender(Layout layout, String filename, String datePattern) throws IOException {
		super(layout, filename, true);
		this.datePattern = datePattern;
		activateOptions();
	}

	/**
	 * The <b>DatePattern</b> takes a string in the same format as expected by
	 * {@link SimpleDateFormat}. This options determines the rollover schedule.
	 */
	public void setDatePattern(String pattern) {
		datePattern = pattern;
	}

	/** Returns the value of the <b>DatePattern</b> option. */
	public String getDatePattern() {
		return datePattern;
	}

	public void activateOptions() {
		super.activateOptions();
		if (datePattern != null && fileName != null) {
			now.setTime(System.currentTimeMillis());
			sdf = new SimpleDateFormat(datePattern);
			int type = computeCheckPeriod();
			printPeriodicity(type);
			rc.setType(type);
			File file = new File(fileName);
			scheduledFilename = fileName + sdf.format(new Date(file.lastModified()));

		} else {
			LogLog.error("Either File or DatePattern options are not set for appender [" + name + "].");
		}
	}

	void printPeriodicity(int type) {
		switch (type) {
		case TOP_OF_MINUTE:
			LogLog.debug("Appender [" + name + "] to be rolled every minute.");
			break;
		case TOP_OF_HOUR:
			LogLog.debug("Appender [" + name + "] to be rolled on top of every hour.");
			break;
		case HALF_DAY:
			LogLog.debug("Appender [" + name + "] to be rolled at midday and midnight.");
			break;
		case TOP_OF_DAY:
			LogLog.debug("Appender [" + name + "] to be rolled at midnight.");
			break;
		case TOP_OF_WEEK:
			LogLog.debug("Appender [" + name + "] to be rolled at start of week.");
			break;
		case TOP_OF_MONTH:
			LogLog.debug("Appender [" + name + "] to be rolled at start of every month.");
			break;
		default:
			LogLog.warn("Unknown periodicity for appender [" + name + "].");
		}
	}

	int computeCheckPeriod() {
		RollingCalendar rollingCalendar = new RollingCalendar(gmtTimeZone, Locale.getDefault());
		// set sate to 1970-01-01 00:00:00 GMT
		Date epoch = new Date(0);
		if (datePattern != null) {
			for (int i = TOP_OF_MINUTE; i <= TOP_OF_MONTH; i++) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
				simpleDateFormat.setTimeZone(gmtTimeZone); // do all date
															// formatting in GMT
				String r0 = simpleDateFormat.format(epoch);
				rollingCalendar.setType(i);
				Date next = new Date(rollingCalendar.getNextCheckMillis(epoch));
				String r1 = simpleDateFormat.format(next);
				// System.out.println("Type = "+i+", r0 = "+r0+", r1 = "+r1);
				if (r0 != null && r1 != null && !r0.equals(r1)) {
					return i;
				}
			}
		}
		return TOP_OF_TROUBLE; // Deliberately head for trouble...
	}

	/**
	 * Rollover the current file to a new file.
	 */
	void rollOver() throws IOException {

		/* Compute filename, but only if datePattern is specified */
		if (datePattern == null) {
			errorHandler.error("Missing DatePattern option in rollOver().");
			return;
		}

		String datedFilename = fileName + sdf.format(now);
		// It is too early to roll over because we are still within the
		// bounds of the current interval. Rollover will occur once the
		// next interval is reached.
		if (scheduledFilename.equals(datedFilename)) {
			return;
		}

		// close current file, and rename it to datedFilename
		this.closeFile();

		File target = new File(scheduledFilename);
		if (target.exists()) {
			target.delete();
		}

		File file = new File(fileName);
		boolean result = file.renameTo(target);
		if (result) {
			LogLog.debug(fileName + " -> " + scheduledFilename);
		} else {
			LogLog.error("Failed to rename [" + fileName + "] to [" + scheduledFilename + "].");
		}

		try {
			// This will also close the file. This is OK since multiple
			// close operations are safe.
			this.setFile(fileName, true, this.bufferedIO, this.bufferSize);
		} catch (IOException e) {
			errorHandler.error("setFile(" + fileName + ", true) call failed.");
		}
		scheduledFilename = datedFilename;
		deleteFile();
	}

	private void deleteFile() {
		File file = new File(fileName);
		File logPath = file.getParentFile();
		System.out.println("获取日志保存路径：" + logPath.getPath());
		System.out.println(this.datePattern);
		File[] fileList = logPath.listFiles();
		for (File tmp : fileList) {
			String tmpFileName = tmp.getName();
			if (tmpFileName.indexOf(".") < 0)
				continue; // 过滤今天的文件
			String date = tmpFileName.substring(tmpFileName.indexOf("_") + 1, tmpFileName.indexOf("."));
			SimpleDateFormat tmpSdf = new SimpleDateFormat(
					this.datePattern.substring(0, this.datePattern.indexOf("'")));
			try {
				int index = differentDaysByMillisecond(now, tmpSdf.parse(date));
				if (index > MaxSize) {
					tmp.delete();
				}
			} catch (ParseException e) {
				e.printStackTrace();
				errorHandler.error("delete file error:" + e.getMessage());
			}
		}
	}

	private int differentDaysByMillisecond(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);

		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		int day1 = cal1.get(Calendar.DAY_OF_YEAR);
		int day2 = cal2.get(Calendar.DAY_OF_YEAR);

		int year1 = cal1.get(Calendar.YEAR);
		int year2 = cal2.get(Calendar.YEAR);
		if (year1 != year2) // 同一年
		{
			int timeDistance = 0;
			for (int i = year1; i < year2; i++) {
				if (i % 4 == 0 && i % 100 != 0 || i % 400 == 0) // 闰年
				{
					timeDistance += 366;
				} else // 不是闰年
				{
					timeDistance += 365;
				}
			}

			return timeDistance + (day2 - day1);
		} else // 不同年
		{
			System.out.println("判断day2 - day1 : " + (day2 - day1));
			return day2 - day1;
		}
	}

	/**
	 * This method differentiates DailyRollingFileAppender from its super class.
	 *
	 * <p>
	 * Before actually logging, this method will check whether it is time to do
	 * a rollover. If it is, it will schedule the next rollover time and then
	 * rollover.
	 */
	protected void subAppend(LoggingEvent event) {
		long n = System.currentTimeMillis();
		if (n >= nextCheck) {
			now.setTime(n);
			nextCheck = rc.getNextCheckMillis(now);
			try {
				rollOver();
			} catch (IOException ioe) {
				if (ioe instanceof InterruptedIOException) {
					Thread.currentThread().interrupt();
				}
				LogLog.error("rollOver() failed.", ioe);
			}
		}
		super.subAppend(event);
	}
}

/**
 * RollingCalendar is a helper class to DailyRollingFileAppender. Given a
 * periodicity type and the current time, it computes the start of the next
 * interval.
 */
class RollingCalendar extends GregorianCalendar {
	private static final long serialVersionUID = -3560331770601814177L;

	int type = MyDailyRollingFileAppender.TOP_OF_TROUBLE;

	RollingCalendar() {
		super();
	}

	RollingCalendar(TimeZone tz, Locale locale) {
		super(tz, locale);
	}

	void setType(int type) {
		this.type = type;
	}

	public long getNextCheckMillis(Date now) {
		return getNextCheckDate(now).getTime();
	}

	public Date getNextCheckDate(Date now) {
		this.setTime(now);

		switch (type) {
		case MyDailyRollingFileAppender.TOP_OF_MINUTE:
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.MINUTE, 1);
			break;
		case MyDailyRollingFileAppender.TOP_OF_HOUR:
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.HOUR_OF_DAY, 1);
			break;
		case MyDailyRollingFileAppender.HALF_DAY:
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			int hour = get(Calendar.HOUR_OF_DAY);
			if (hour < 12) {
				this.set(Calendar.HOUR_OF_DAY, 12);
			} else {
				this.set(Calendar.HOUR_OF_DAY, 0);
				this.add(Calendar.DAY_OF_MONTH, 1);
			}
			break;
		case MyDailyRollingFileAppender.TOP_OF_DAY:
			this.set(Calendar.HOUR_OF_DAY, 0);
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.DATE, 1);
			break;
		case MyDailyRollingFileAppender.TOP_OF_WEEK:
			this.set(Calendar.DAY_OF_WEEK, getFirstDayOfWeek());
			this.set(Calendar.HOUR_OF_DAY, 0);
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.WEEK_OF_YEAR, 1);
			break;
		case MyDailyRollingFileAppender.TOP_OF_MONTH:
			this.set(Calendar.DATE, 1);
			this.set(Calendar.HOUR_OF_DAY, 0);
			this.set(Calendar.MINUTE, 0);
			this.set(Calendar.SECOND, 0);
			this.set(Calendar.MILLISECOND, 0);
			this.add(Calendar.MONTH, 1);
			break;
		default:
			throw new IllegalStateException("Unknown periodicity type.");
		}
		return getTime();
	}
}
