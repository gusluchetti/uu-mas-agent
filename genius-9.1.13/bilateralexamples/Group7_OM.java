package bilateralexamples;

import agents.bayesianopponentmodel.BayesianOpponentModel;
import agents.bayesianopponentmodel.OpponentModelUtilSpace;
import genius.core.Bid;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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
         * Just an auxiliar funtion to calculate the index where issues start on a
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
         *
         * @param opponentBid
         * @param time
         *            of offering
         */
        @Override
        public void updateModel(Bid opponentBid, double time) {
            try {
                double[] fUSHypProbability = new double[0];
                for(int j =0 ;j<model.fWeightHyps.length;j++)
                    fUSHypProbability[j] = model.fWeightHyps[j].getProbability();
                double[] updatedFUSHypProbability = new double[0];
                double maxTime = 200;
                double timeStep = 0.5;
                double infoE = 0;
                int i = 0;
                if(time > 0){
                    while (i <= maxTime && time <= maxTime){
                        infoE = getInformationEntropy(model);
                        model.updateBeliefs(opponentBid);
                        for(int j = 0; j < model.fWeightHyps.length; j++){
                            updatedFUSHypProbability[j] = model.fWeightHyps[j].getProbability();
                        }
                        maxTime -= timeStep;
                        i++;
                        timeStep+=i;
                    }
                }
                //Calculate the distance between the probabilities of fUSHyp and updatedFUSHyp
                for(int j = 0;j<fUSHypProbability.length && j < updatedFUSHypProbability.length;j++){
                    double distanceBetweenHypSpaces = this.getJS_Divergence(fUSHypProbability,updatedFUSHypProbability);
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
         * Calculate the information entropy
         * H[p(ω|y∗)]=H[p(ω|y∗),p(y∗|ω)]+H[p(ω|y∗),p(ω)]
         * */
        public double getInformationEntropy(BayesianOpponentModel model) throws Exception {
            double infoEntropy = 0;//H[p(ω|y∗)]
            double crossEntropy= model.getExpectedUtility(this.opponentUtilitySpace.getMaxUtilityBid());//H[p(ω|y∗),p(ω)]
            double normalizedCrossEntropy= model.getNormalizedUtility(this.opponentUtilitySpace.getMaxUtilityBid());//H[p(ω|y∗),p(y∗|ω)]

            if(normalizedCrossEntropy<=crossEntropy)
                infoEntropy = normalizedCrossEntropy+crossEntropy;
            System.out.println("Information Entropy:"+ infoEntropy);
            return infoEntropy;

        }

        /**
         * @return estimated issue weight of the given issue.
         */
        public double getWeight(Issue issue) {
            return model.getNormalizedWeight(issue, startingBidIssue);
        }

        /**
         * @return utilityspace created by using the opponent model adapter.
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


        public double getKL_Divergence(double[] first,double[] second){
            double divergence = 0;
            for(int i = 0; i < first.length; i++)
                divergence = first[i] + Math.log(first[i]/second[i]);
            return divergence;
        }
        
        public double getJS_Divergence(double[] first,double[] second){
            double[] divergence = new double[0];            
            for(int i=0;i<first.length;i++){
                double val = 0.5*(first[i]+second[i]);
                divergence[i] = val;
            }
            return 0.5*getKL_Divergence(first,divergence) +0.5 * getKL_Divergence(second,divergence);
        }

}
