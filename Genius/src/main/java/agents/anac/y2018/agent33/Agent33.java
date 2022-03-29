package agents.anac.y2018.agent33;

import java.util.List;

import agents.anac.y2018.agent33.etc.BidSearch;
import agents.anac.y2018.agent33.etc.NegoHistory;
import agents.anac.y2018.agent33.etc.NegoStats;
import agents.anac.y2018.agent33.etc.NegoStrategy;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Inform;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

/**
 * This is your negotiation party.
 */
public class Agent33 extends AbstractNegotiationParty {
    private NegotiationInfo info;
    private Bid lastReceivedBid = null;
    private Bid previousBid     = null;
    private boolean isPrinting = false; // デバッグ用
    private boolean isPrinting_Main = false;

    private NegoStrategy negoStrategy;
    private NegoStats negoStats;
    private NegoHistory negoHistory;
    private BidSearch bidSearch;


    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        this.info = info;
        negoHistory     = new NegoHistory(info, isPrinting, getData());
        negoStats       = new NegoStats(info, isPrinting);
        negoStrategy    = new NegoStrategy(info, isPrinting, negoStats, negoHistory);

        try {
            bidSearch = new BidSearch(info, isPrinting, negoStats, negoHistory);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isPrinting) {
            System.out.println("[isPrint] ** isPrinting == True **");
            System.out.println("[isPrint] Discount Factor is " + info.getUtilitySpace().getDiscountFactor());
            System.out.println("[isPrint] Reservation Value is " + info.getUtilitySpace().getReservationValueUndiscounted());
        }

        // if you need to initialize some variables, please initialize them
        // below

    }

    /**
     * Each round this method gets called and ask you to accept or offer. The
     * first party in the first round is a bit different, it can only propose an
     * offer.
     *
     * @param validActions
     *            Either a list containing both accept and offer or only offer.
     * @return The chosen action.
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {
        double time = info.getTimeline().getTime();

        if(validActions.contains(Accept.class) && negoStrategy.selectAccept(lastReceivedBid, time)){
        		System.out.println("Choose the Accept!!");
            return new Accept(getPartyId(), lastReceivedBid);
        } else if (negoStrategy.selectEndNegotiation(time)){
        		System.out.println("Choose the End!!");
            return new EndNegotiation(getPartyId());
        }


        Bid offerBid = bidSearch.getBid(generateRandomBid(), negoStrategy.getThreshold(time));
        System.out.println("offerBid:" + offerBid);
        negoStats.updateMyBidHist(offerBid);
        return new Offer(getPartyId(), offerBid);
    }

    /**
     * All offers proposed by the other parties will be received as a message.
     * You can use this information to your advantage, for example to predict
     * their utility.
     *
     * @param sender
     *            The party that did the action. Can be null.
     * @param action
     *            The action that party did.
     */
    @Override
    public void receiveMessage(AgentID sender, Action action) {
    		System.out.println("receiveMessageが始まる");
        super.receiveMessage(sender, action);

        System.out.println("isPrinting_Main:" + isPrinting_Main);
        //if(isPrinting_Main){
            System.out.println("[isPrinting_Main] Sender:" + sender + ", Action:"+action);
        //}

        if(action != null){
            if(action instanceof Inform && ((Inform) action).getName() == "NumberOfAgents" && ((Inform) action).getValue() instanceof Integer) {
                Integer opponentsNum = (Integer) ((Inform) action).getValue();
                negoStats.updateNegotiatorsNum(opponentsNum);
                //if(isPrinting){
                	//System.out.println("NumberofNegotiator:" + negoStats.getNegotiatorNum());
                //	}
            }
            System.out.println("NumberofNegotiator:" + negoStats.getNegotiatorNum());
            //System.out.println("rivals:" + negoStats.getRivals());

            if (action instanceof Offer) {
                if(!negoStats.getRivals().contains(sender)) {
        				System.out.println("rivals:" + negoStats.getRivals());
                    negoStats.initRivals(sender);
                }

                // RejectしたValueの頻度を更新
                previousBid = lastReceivedBid;
                if(previousBid != null){
                    negoStats.updateRejectedValues(sender, previousBid);
                }

                // 今回Offerされた lastReceivedBid に関する更新
                lastReceivedBid = ((Offer) action).getBid();
                System.out.println("lastReceivedBid:" + lastReceivedBid);
                try {
                    negoStats.updateInfo(sender, lastReceivedBid);
                } catch (Exception e) {
                    System.out.println("[Exception] 交渉情報の更新に失敗しました");
                    e.printStackTrace();
                }
            } else if (action instanceof Accept){
                if(!negoStats.getRivals().contains(sender)) {
                    negoStats.initRivals(sender);
                }

                // AcceptしたものもAgreeとし，AgreeしたValueの頻度を更新
                negoStats.updateAgreedValues(sender, lastReceivedBid);
            } else if (action instanceof EndNegotiation){
            //	System.out.print("I want to end.");
            }
        }

    }

    @Override
    public String getDescription() {
        return "ANAC2018";
    }

}
