package src.mas2022.group7;

import agents.bayesianopponentmodel.OpponentModelUtilSpace;
import genius.core.Bid;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import src.mas2022.group7.bayesianopponentmodel.BayesianOpponentModel;

import java.util.*;


public class Group7_OM extends OpponentModel {

    // This code was based on the BayesianOpponentModel.class

    /**
     * Adapter for BayesianModel. Note that this model only works on small domains.
     *
     * Adapted by Mark Hendrikx to be compatible with the BOA framework.
     *
     * Tim Baarslag, Koen Hindriks, Mark Hendrikx, Alex Dirkzwager and Catholijn M.
     * Jonker. Decoupling Negotiating Agents to Explore the Space of Negotiation
     * Strategies
     *
     * @author Mark Hendrikx
     */


        /** Reference to the normal Bayesian Opponent Model */
        private BayesianOpponentModel model;
        /** Index of the first issue weight */
        private int startingBidIssue = 0;


        @Override
        public String getName() {
            return "2022 - Agent007 Opponent Model";
        }

    /**
     * Initializes the opponent model. If the parameter m is set to a value
         * greater than zero, only the best hypothesis about the opponent's utility
         * space is used.
         */
        @Override
        public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
            this.negotiationSession = negotiationSession;
            model = new BayesianOpponentModel((AdditiveUtilitySpace) negotiationSession.getUtilitySpace());
            if (parameters.get("m") != null) {
                model.setMostProbableUSHypsOnly(parameters.get("m") > 0);
            } else {
                model.setMostProbableUSHypsOnly(false);
                System.out.println("Constant \"m\" was not set. Assumed default value.");
            }

            while (!testIndexOfFirstIssue(negotiationSession.getUtilitySpace().getDomain().getRandomBid(null),
                    startingBidIssue)) {
                startingBidIssue++;
            }
        }

        /**
         * Just an auxiliar function to calculate the index where issues start on a
         * bid because we found out that it depends on the domain.
         *
         * @return true when the received index is the proper index
         */
        private boolean testIndexOfFirstIssue(Bid bid, int i) {
            try {
                @SuppressWarnings("unused")
                ValueDiscrete valueOfIssue = (ValueDiscrete) bid.getValue(i);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        /**
         * Update the opponent model by updating all hypotheses about the opponent's
         * preference profile.
         */
        @Override
        public void updateModel(Bid opponentBid, double time) {
            try {
                List<Double> fUSHypProbability = new ArrayList<>();
                List<Double> updatedFUSHypProbability = new ArrayList<>();
                double maxTime = 200;
                double timeStep = 0.5;
                int i = 0;

                for(int j = 0; j < model.fWeightHyps.length; j++) {
                    fUSHypProbability.add(model.fWeightHyps[j].getProbability());
                }

                if(time > 0){
                    while (i <= maxTime && time <= maxTime){
                        model.updateBeliefs(opponentBid);
                        for(int j = 0; j < model.fWeightHyps.length; j++) {
                            updatedFUSHypProbability.add(model.fWeightHyps[j].getProbability());
                        }
                        maxTime -= timeStep;
                        i++;
                        timeStep += i;
                    }
                }

                // Calculate the distance between the probabilities of fUSHyp and updatedFUSHyp
                for(int j = 0; j<fUSHypProbability.size() && j < updatedFUSHypProbability.size(); j++){
                    double distanceBetweenHypSpaces = this.getJS_Divergence(fUSHypProbability, updatedFUSHypProbability);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public double getBidEvaluation(Bid bid) {
            try {
                return model.getNormalizedUtility(bid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        /**
         * @return estimated issue weight of the given issue.
         */
        public double getWeight(Issue issue) {
            return model.getNormalizedWeight(issue, startingBidIssue);
        }

        /**
         * @return utility space created by using the opponent model adapter.
         */
        @Override
        public AdditiveUtilitySpace getOpponentUtilitySpace() {
            return new OpponentModelUtilSpace(model);
        }

        public void cleanUp() {
            super.cleanUp();
        }

        @Override
        public Set<BOAparameter> getParameterSpec() {
            Set<BOAparameter> set = new HashSet<BOAparameter>();
            set.add(new BOAparameter("m", 0.0, "If higher than 0 the most probable hypothesis is only used"));
            return set;
        }

        public double getJS_Divergence(List<Double> first, List<Double> second){
            List<Double> divergence = new ArrayList<>();
            for(int i = 0; i < first.size()-1; i++){
                double val = (first.get(i)+second.get(i))/2;
                divergence.add(val);
            }
            return getKL_Divergence(first, divergence)/2 + getKL_Divergence(second, divergence)/2;
        }

        public double getKL_Divergence(List<Double> first, List<Double> second){
            double divergence = 0;
            for(int i = 0; i < first.size()-1; i++)
                divergence = first.get(i) + Math.log(first.get(i)/second.get(i));
            return divergence;
        }
}
