package bilateralexamples;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.OMStrategy;

import java.util.List;

public class Group7_OMS extends OMStrategy {
    @Override
    public BidDetails getBid(List<BidDetails> bidsInRange) {
        return null;
    }

    @Override
    public boolean canUpdateOM() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }
}
