package bilateralexamples.mas2022.group7;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.OfferingStrategy;

public class Group7_BS extends OfferingStrategy{
    @Override
    public BidDetails determineOpeningBid() {
        return this.determineNextBid();
    }

    @Override
    public BidDetails determineNextBid() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
