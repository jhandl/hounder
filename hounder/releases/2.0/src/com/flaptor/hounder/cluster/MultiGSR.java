package com.flaptor.hounder.cluster;

import java.io.Serializable;
import java.util.ArrayList;

import com.flaptor.hounder.searcher.GroupedSearchResults;

/**
 * This class is used by the multisearcher to return not only the search results (GSR),
 *  but also data about the searchers: how many results returned each searcher, timeouts or time spent, etc.
 * @author rafa
 *
 */
public class MultiGSR implements Serializable{
	private static final long serialVersionUID = 1L;

	private final GroupedSearchResults gsr;
	private ArrayList<ResponseData> responsesData;


	public MultiGSR(GroupedSearchResults gsr, int numSearchers) {
		this.gsr= gsr;
		responsesData= new ArrayList<ResponseData>(numSearchers);
        ResponseData timeout = new ResponseData(-1, -1);
		for (int i=0; i< numSearchers; i++){
			responsesData.add(timeout);
		}
	}

	public int getNumOfSerachers(){
		return responsesData.size();
	}

	/**
	 *
	 * @param searcherNum
	 * @param time -1 for timeout
	 * @param numOfResults -1 for timeout
	 */
	public void setData(int searcherNum, Long time, Integer numOfResults){
		responsesData.set(searcherNum, new ResponseData(time, numOfResults));
	}

	/**
	 *
	 * @param searcherNum
	 * @return -1 for timeout
	 */
	public Long getResponseTime(int searcherNum) {
		return responsesData.get(searcherNum).getResponseTime();
	}

	/**
	 *
	 * @param searcherNum
	 * @return -1 for timeout
	 */
	public Integer getResponseResults(int searcherNum){
		return responsesData.get(searcherNum).getResponseResults();
	}

	public GroupedSearchResults getGsr() {
		return gsr;
	}

	public class ResponseData{
		private long responseTime;
		private int responseResults;

		public ResponseData(long responseTime, int responseResults) {
			this.responseTime = responseTime;
			this.responseResults = responseResults;
		}

		public long getResponseTime() {
			return responseTime;
		}

		public int getResponseResults() {
			return responseResults;
		}


	}
}
