package aigenetics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Population implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public List<HeuristicWeights> individuals;

    public Population(int size, boolean init) {
        individuals = new ArrayList<>(size);
        if (init) {
            for (int i = 0; i < size; i++) {
                individuals.add(new HeuristicWeights());
            }
        }
    }

    public void add(HeuristicWeights h) {
        individuals.add(h);
    }

    public HeuristicWeights get(int i) {
        return individuals.get(i);
    }

    public int size() {
        return individuals.size();
    }

    public HeuristicWeights getFittest() {
        return individuals.stream()
                .max(Comparator.comparingDouble(HeuristicWeights::getFitness))
                .orElse(null);
    }

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();;
		for (HeuristicWeights a : this.individuals) {
			sb.append(a.toString());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}
}