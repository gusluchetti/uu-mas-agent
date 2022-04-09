/*
 * OwnBidHistory class
 */
package agents.anac.y2015.Mercury;

import java.util.ArrayList;

import genius.core.Bid;
import genius.core.utility.AdditiveUtilitySpace;

/**
 *
 * @author Justin
 */
public class OwnBidHistory {

    private ArrayList<Bid> BidHistory;
    private Bid minBidInHistory= null;
    private Bid lastBid = null;
    
    public OwnBidHistory() {
        BidHistory = new ArrayList<Bid>();
    }

    public void addBid(Bid bid, AdditiveUtilitySpace utilitySpace) {
    	lastBid = bid;
    	
    	if (BidHistory.indexOf(bid) == -1) {
            BidHistory.add(bid);
        }else if ( !isInsideMyBids(bid) ){
        	BidHistory.add(bid);
        }else{
        	
        }
    	
        /*try {
            if (BidHistory.size() == 1) {
                this.minBidInHistory = BidHistory.get(0);
            } else {
                if (utilitySpace.getUtility(bid) < utilitySpace.getUtility(this.minBidInHistory)) {
                    this.minBidInHistory = bid;
                }
            }
        } catch (Exception e) {
            System.out.println("error in add Bid method of OwnBidHistory class" + e.getMessage());
        }*/
    }
    
    public boolean isInsideMyBids(Bid a){
		boolean result = false;
		
		for(int i = 0; i < BidHistory.size(); i++){
			if(a.equals(BidHistory.get(i))){
				return true;
			}
		}
		
		return result;
	}
    
    protected Bid GetMinBidInHistory() {

        return this.minBidInHistory;
    }

    protected Bid getLastBid() {
       /* if (BidHistory.size() >= 1) {
            return BidHistory.get(BidHistory.size() - 1);
        } else {
            return null;
        }*/
        
        return lastBid;
    }

    public int numOfBidsProposed() {
        return BidHistory.size();
    }

    protected Bid chooseLowestBidInHistory(AdditiveUtilitySpace utilitySpace) {
        double minUtility = 100;
        Bid minBid = null;
        try {
            for (Bid bid : BidHistory) {
                if (utilitySpace.getUtility(bid) < minUtility) {
                    minUtility = utilitySpace.getUtility(bid);
                    minBid = bid;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception in chooseLowestBidInHistory");
        }
        return minBid;
    }
}
