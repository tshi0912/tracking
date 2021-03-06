/**
 * 
 */
package com.dianping.cat.report.task.transaction;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.dainping.cat.consumer.dal.report.Report;
import com.dianping.cat.Cat;
import com.dianping.cat.consumer.transaction.TransactionReportUrlFilter;
import com.dianping.cat.consumer.transaction.model.entity.TransactionReport;
import com.dianping.cat.consumer.transaction.model.transform.DefaultSaxParser;
import com.dianping.cat.report.page.model.transaction.TransactionReportMerger;
import com.dianping.cat.report.task.TaskHelper;
import com.dianping.cat.report.task.spi.ReportMerger;

public class TransactionMerger implements ReportMerger<TransactionReport> {

	private TransactionReport merge(String reportDomain, List<Report> reports, boolean isDaily) {
		TransactionReportMerger merger = null;
		if (isDaily) {
			merger = new HistoryTransactionReportMerger(new TransactionReport(reportDomain));
		} else {
			merger = new TransactionReportMerger(new TransactionReport(reportDomain));
		}
		for (Report report : reports) {
			String xml = report.getContent();
			TransactionReport model;
			try {
				model = DefaultSaxParser.parse(xml);
				model.accept(merger);
			} catch (Exception e) {
				Cat.logError(e);
			}
		}

		TransactionReport transactionReport = merger.getTransactionReport();
		return transactionReport;
	}

	public TransactionReport mergeForDaily(String reportDomain, List<Report> reports, Set<String> domainSet) {
		TransactionReport transactionReport = merge(reportDomain, reports, true);
		HistoryTransactionReportMerger merger = new HistoryTransactionReportMerger(new TransactionReport(reportDomain));
		TransactionReport transactionReport2 = merge(reportDomain, reports, true);
		com.dianping.cat.consumer.transaction.model.entity.Machine allMachines = merger
		      .mergesForAllMachine(transactionReport2);
		transactionReport.addMachine(allMachines);
		transactionReport.getIps().add("All");
		transactionReport.getDomainNames().addAll(domainSet);

		Date date = transactionReport.getStartTime();
		transactionReport.setStartTime(TaskHelper.todayZero(date));
		Date end = new Date(TaskHelper.tomorrowZero(date).getTime() - 1000);
		transactionReport.setEndTime(end);

		new TransactionReportUrlFilter().visitTransactionReport(transactionReport);
		return transactionReport;
	}

	public TransactionReport mergeForGraph(String reportDomain, List<Report> reports) {
		TransactionReport transactionReport = merge(reportDomain, reports, false);
		TransactionReportMerger merger = new TransactionReportMerger(new TransactionReport(reportDomain));
		TransactionReport transactionReport2 = merge(reportDomain, reports, false);
		com.dianping.cat.consumer.transaction.model.entity.Machine allMachines = merger
		      .mergesForAllMachine(transactionReport2);
		transactionReport.addMachine(allMachines);
		transactionReport.getIps().add("All");

		new TransactionReportUrlFilter().visitTransactionReport(transactionReport);
		return transactionReport;
	}
}
