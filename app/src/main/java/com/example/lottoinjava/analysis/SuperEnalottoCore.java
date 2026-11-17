package com.example.lottoinjava.analysis;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analisi SuperEnalotto: copia della logica della versione Linux (SuperEnalottoDialog) senza JavaFX, per Android.
 * Archivio con 6 numeri per estrazione (Jolly e SuperStar facoltativi). Le statistiche storiche non cambiano le probabilità.
 */
public final class SuperEnalottoCore {

    private SuperEnalottoCore() { }

    public static final String[] URLS = {
            "https://www.estrazioni.it/index.php?formato=csv&p=download&tipo=superenalotto",
            "https://raw.githubusercontent.com/luigimassa/superenalotto-archivio/main/superenalotto.csv"
    };

    /** Una estrazione. */
    public static final class Draw {public final LocalDate date;public final String contest;public final int[] numbers;public final String jolly,superstar;Draw(LocalDate d,String c,int[] n,String j,String s){date=d;contest=c==null?"":c;numbers=n;jolly=j==null?"":j;superstar=s==null?"":s;}public boolean has(int n){for(int x:numbers)if(x==n)return true;return false;}public String key(){return date.toString();}}

    private static String postReport(List<Draw> d,int spia,int k){Map<Integer,Integer> freq=new HashMap<>(),cycles=new HashMap<>();Map<Integer,List<Integer>> pos=new HashMap<>();int events=0;
        for(int i=0;i<d.size();i++)if(d.get(i).has(spia)&&i+k<d.size()){events++;Set<Integer> seen=new HashSet<>();Map<Integer,Integer> firstPosition=new HashMap<>();for(int j=i+1;j<=i+k;j++){Draw x=d.get(j);for(int n:x.numbers){freq.merge(n,1,Integer::sum);seen.add(n);firstPosition.putIfAbsent(n,j-i);}}for(int n:seen){cycles.merge(n,1,Integer::sum);pos.computeIfAbsent(n,z->new ArrayList<>()).add(firstPosition.get(n));}}
        if(events==0)return "Nessun ciclo post-spia completo per il numero "+spia+" nel periodo.";
        final int ev=events;List<Integer> top=rank(freq,cycles,pos,ev,k,spia);StringBuilder s=new StringBuilder("ANALISI POST-SPIA\nSpia: "+fmt(spia)+" · estrazioni "+d.size()+" · cicli completi "+events+" · colpi "+k+"\n\nTOP NUMERI PER SCORE (frequenza 40%, cicli 40%, posizione 20%)\n");for(int n:top)s.append(String.format(Locale.ROOT,"%02d  freq=%d  cicli=%d/%d  pos.media=%.2f  score=%.2f%n",n,freq.getOrDefault(n,0),cycles.getOrDefault(n,0),events,avg(pos.get(n)),score(n,freq,cycles,pos,events,k)));s.append("\nNota: le statistiche storiche non aumentano la probabilità matematica dell'estrazione.");return s.toString();}
    private static List<Integer> rank(Map<Integer,Integer> freq,Map<Integer,Integer> cycles,Map<Integer,List<Integer>> pos,int events,int k,int spia){double max=freq.values().stream().mapToInt(Integer::intValue).max().orElse(1);return java.util.stream.IntStream.rangeClosed(1,90).filter(n->n!=spia&&freq.getOrDefault(n,0)>0).boxed().sorted(Comparator.<Integer>comparingDouble(n->-score(n,freq,cycles,pos,events,k)).thenComparingInt(n->-freq.getOrDefault(n,0)).thenComparingInt(n->n)).collect(Collectors.toList());}
    private static double score(int n,Map<Integer,Integer> f,Map<Integer,Integer> c,Map<Integer,List<Integer>> p,int ev,int k){double max=f.values().stream().mapToInt(Integer::intValue).max().orElse(1);return f.getOrDefault(n,0)/max*40+c.getOrDefault(n,0)/(double)Math.max(1,ev)*40+(p.containsKey(n)?(k+1-avg(p.get(n)))/k*20:0);}
    private static String forecastReport(List<Draw> d,int spia){return forecastReport(d,spia,true);}
    /** Con conSestine=false salta le sestine candidate (servono solo nel rapporto, non nei backtest: i numeri TOP sono identici). */
    private static String forecastReport(List<Draw> d,int spia,boolean conSestine){if(d.size()<30)throw new IllegalArgumentException("Il motore statistico richiede almeno 30 estrazioni nel periodo.");int window=Math.min(120,Math.max(30,d.size()/5));Map<Integer,Integer> freq=new HashMap<>(),recent=new HashMap<>(),cycle=new HashMap<>(),co=new HashMap<>();int[] delay=new int[91];Arrays.fill(delay,d.size());
        for(int i=0;i<d.size();i++){Draw x=d.get(i);for(int n:x.numbers){freq.merge(n,1,Integer::sum);if(i>=d.size()-window)recent.merge(n,1,Integer::sum);delay[n]=d.size()-1-i;}}
        for(int i=0;i<d.size();i++)if(d.get(i).has(spia)){Set<Integer> seen=new HashSet<>();for(int j=i+1;j<Math.min(d.size(),i+10);j++)for(int n:d.get(j).numbers)seen.add(n);for(int n:seen)co.merge(n,1,Integer::sum);for(int j=i+1;j<Math.min(d.size(),i+10);j++)for(int n:d.get(j).numbers)cycle.merge(n,1,Integer::sum);}
        double[] fn=norm(freq),rn=norm(recent),cn=norm(cycle),sn=norm(co);int maxDelay=Arrays.stream(delay,1,91).max().orElse(1);StringBuilder s=new StringBuilder("PREVISIONE STATISTICA · finestra recente "+window+" estrazioni\nSpia "+fmt(spia)+" · periodo "+d.get(0).date+" – "+d.get(d.size()-1).date+"\n\nPesi come nel main.py: frequenza 25%, recenza 25%, presenza nei cicli 25%, co-presenza spia 15%, ritardo 10%.\nIl ritardo è informativo, non una certezza.\n\nTOP 15 SCORE\n");List<Integer> nums=new ArrayList<>();for(int n=1;n<=90;n++)nums.add(n);nums.sort(Comparator.<Integer>comparingDouble(n->-(.25*fn[n]+.25*rn[n]+.25*cn[n]+.15*sn[n]+.10*delay[n]/(double)Math.max(1,maxDelay))).thenComparingInt(n->n));for(int i=0;i<15;i++){int n=nums.get(i);double sc=(.25*fn[n]+.25*rn[n]+.25*cn[n]+.15*sn[n]+.10*delay[n]/(double)Math.max(1,maxDelay))*100;s.append(String.format(Locale.ROOT,"%02d  score %.2f · freq %d · recenti %d · cicli %d · co-spia %d · ritardo %d%n",n,sc,freq.getOrDefault(n,0),recent.getOrDefault(n,0),cycle.getOrDefault(n,0),co.getOrDefault(n,0),delay[n]));}s.append("\nPANIERE TOP 15: ").append(nums.subList(0,15).stream().map(SuperEnalottoCore::fmt).collect(Collectors.joining(" - "))).append("\nSestine candidate: combinazioni TOP 15 ordinate per score medio (prime 10)\n");if(!conSestine)return s.toString();List<Integer> comboPool=new ArrayList<>(nums.subList(0,15));if(comboPool.remove(Integer.valueOf(spia)))comboPool.add(spia);List<List<Integer>> combos=new ArrayList<>();combine(comboPool,0,6,new ArrayList<>(),combos);Map<String,Integer> pair=new HashMap<>();for(Draw x:d)for(int i=0;i<6;i++)for(int j=i+1;j<6;j++)pair.merge(Math.min(x.numbers[i],x.numbers[j])+":"+Math.max(x.numbers[i],x.numbers[j]),1,Integer::sum);combos.sort(Comparator.<List<Integer>>comparingDouble(c->-forecastComboScore(c,fn,rn,cn,sn,delay,maxDelay,pair)));for(int i=0;i<Math.min(10,combos.size());i++)s.append(String.format("%02d. %s%n",i+1,combos.get(i).stream().map(SuperEnalottoCore::fmt).collect(Collectors.joining(" "))));return s.toString();}
    private static double forecastComboScore(List<Integer> combo,double[] fn,double[] recent,double[] cycle,double[] co,int[] delay,int maxDelay,Map<String,Integer> pairs) {
        double scoreNum=combo.stream().mapToDouble(n->(.25*fn[n]+.25*recent[n]+.25*cycle[n]+.15*co[n]+.10*delay[n]/(double)Math.max(1,maxDelay))*100).average().orElse(0);
        int even=0,sum=0;Set<Integer> decades=new HashSet<>();int pairScore=0;
        for(int n:combo){if(n%2==0)even++;sum+=n;decades.add((n-1)/10);}
        for(int i=0;i<combo.size();i++)for(int j=i+1;j<combo.size();j++){int a=Math.min(combo.get(i),combo.get(j)),b=Math.max(combo.get(i),combo.get(j));pairScore+=pairs.getOrDefault(a+":"+b,0);}
        double balance=(even>=2&&even<=4)?1.0:.55;
        double distribution=Math.min(1.0,decades.size()/5.0);
        double sumBonus=(sum>=120&&sum<=330)?1.0:.60;
        return scoreNum*.70+balance*10+distribution*10+sumBonus*10+Math.min(pairScore/30.0,10.0);
    }

    private static String cycloReport(List<Draw> d,int spia,int k) {
        int[] freq=new int[91],distFreq=new int[46],eventsDist=new int[46];
        Map<Integer,List<Integer>> positions=new HashMap<>();
        int events=0;
        for(int i=0;i<d.size();i++) if(d.get(i).has(spia)&&i+k<d.size()) {
            events++; Set<Integer> seenD=new HashSet<>(),seenN=new HashSet<>();
            for(int j=i+1;j<=i+k;j++) {
                Draw x=d.get(j);
                for(int n:x.numbers) if(n!=spia) {
                    int dist=Math.min(Math.abs(n-spia),90-Math.abs(n-spia));
                    freq[n]++; distFreq[dist]++; seenD.add(dist);
                    if(seenN.add(n)) positions.computeIfAbsent(n,z->new ArrayList<>()).add(j-i);
                }
            }
            for(int z:seenD) eventsDist[z]++;
        }
        if(events==0) return "Dati insufficienti per la ciclometria: nessun ciclo completo.";
        List<Integer> ns=new ArrayList<>();
        for(int n=1;n<=90;n++) if(n!=spia) ns.add(n);
        final int totalEvents=events;
        ns.sort(Comparator.<Integer>comparingDouble(n->-cycloScore(n,spia,freq,distFreq,positions,totalEvents,k)).thenComparingInt(n->n));
        StringBuilder out=new StringBuilder("CICLOMETRIA SPIA · distanza circolare su 90 numeri\nSpia "+fmt(spia)+" · eventi "+events+" · colpi "+k+"\n\nTOP 15 NUMERI\n");
        for(int i=0;i<Math.min(15,ns.size());i++) {
            int n=ns.get(i),dist=Math.min(Math.abs(n-spia),90-Math.abs(n-spia));
            out.append(String.format(Locale.ROOT,"%02d · distanza %02d · frequenza %d · eventi distanza %d · score %.2f%n",n,dist,freq[n],eventsDist[dist],cycloScore(n,spia,freq,distFreq,positions,totalEvents,k)));
        }
        out.append("\nANELLI CICLOMETRICI (distanza, frequenza, presenza negli eventi e lift)\n");
        List<Integer> ds=new ArrayList<>(); for(int i=1;i<=45;i++) ds.add(i);
        ds.sort(Comparator.<Integer>comparingDouble(i->{double expected=totalEvents*k*6.0/90.0*(i<45?2:1);return -(distFreq[i]/Math.max(1.0,expected));}).thenComparingInt(i->i));
        for(int i=0;i<Math.min(15,ds.size());i++){int z=ds.get(i);double expected=events*k*6.0/90.0*(z<45?2:1);double lift=expected>0?distFreq[z]/expected:0;out.append(String.format(Locale.ROOT,"d=%02d · occorrenze %d · eventi %d · attese %.2f · lift %.3f%n",z,distFreq[z],eventsDist[z],expected,lift));}
        out.append("\nSCHEDE CICLOMETRICHE DAI TOP 10 (ordinamento statistico, non garanzia)\n");
        List<Integer> top=ns.subList(0,Math.min(10,ns.size()));
        int[] sizes={2,3,4,6},limits={6,6,5,4}; String[] labels={"Coppie","Terni","Quartine","Sestine"};
        for(int ix=0;ix<sizes.length;ix++) {
            int size=sizes[ix]; List<List<Integer>> combos=new ArrayList<>();
            if(top.size()>=size) combine(top,0,size,new ArrayList<>(),combos);
            final int eventCount=totalEvents,shotCount=k;
            combos.sort(Comparator.<List<Integer>>comparingDouble(c->-cycloComboScore(c,spia,freq,distFreq,positions,eventCount,shotCount)));
            out.append("\n").append(labels[ix]).append(" TOP ").append(Math.min(limits[ix],combos.size())).append(":\n");
            for(int j=0;j<Math.min(limits[ix],combos.size());j++) {
                List<Integer> c=combos.get(j);
                out.append(String.format(Locale.ROOT,"%02d. %s · score %.2f%n",j+1,c.stream().map(SuperEnalottoCore::fmt).collect(Collectors.joining(" ")),cycloComboScore(c,spia,freq,distFreq,positions,eventCount,shotCount)));
            }
        }
        return out.toString();
    }

    private static double cycloScore(int n,int spia,int[] freq,int[] distFreq,Map<Integer,List<Integer>> positions,int events,int k) {
        int dist=Math.min(Math.abs(n-spia),90-Math.abs(n-spia));
        int maxf=Arrays.stream(freq).max().orElse(1),maxd=Arrays.stream(distFreq).max().orElse(1);
        List<Integer> p=positions.getOrDefault(n,Collections.<Integer>emptyList()); double mean=avg(p);
        double variance=p.stream().mapToDouble(x->(x-mean)*(x-mean)).average().orElse(0);
        double stability=p.isEmpty()?0:1/(1+Math.sqrt(variance));
        double expected=events*k*6.0/90.0;
        double lift=expected>0?freq[n]/expected:0.0;
        // Stessa scala 0-100 e stessi pesi della ciclometria nel main.py.
        double numberStrength=freq[n]/(double)Math.max(1,maxf);
        double ringStrength=distFreq[dist]/(double)Math.max(1,maxd);
        double liftScore=Math.min(lift,3.0)/3.0;
        return 100.0*(.45*numberStrength+.30*ringStrength+.15*stability+.10*liftScore);
    }

    private static double cycloComboScore(List<Integer> combo,int spia,int[] freq,int[] distFreq,Map<Integer,List<Integer>> positions,int events,int k) {
        double avgScore=combo.stream().mapToDouble(n->cycloScore(n,spia,freq,distFreq,positions,events,k)).average().orElse(0);
        Set<Integer> distances=new HashSet<>(); int min=45,max=1;
        for(int n:combo){int d=Math.min(Math.abs(n-spia),90-Math.abs(n-spia));distances.add(d);min=Math.min(min,d);max=Math.max(max,d);}
        double uniqueBonus=distances.size()/(double)combo.size()*8.0;
        double spreadBonus=Math.min((max-min)/45.0,1.0)*4.0;
        return avgScore+uniqueBonus+spreadBonus;
    }

    private static String unifiedReport(List<Draw>d,int spia,int k){String a=postReport(d,spia,k),b=cycloReport(d,spia,k),c=forecastReport(d,spia);Set<Integer> post=topFromReportNumbers(a,10),cyc=topFromReportNumbers(b,10),forecast=topFromReportNumbers(c,15);Map<Integer,Integer> agree=new HashMap<>();for(int n:post)agree.merge(n,1,Integer::sum);for(int n:cyc)agree.merge(n,1,Integer::sum);for(int n:forecast)agree.merge(n,1,Integer::sum);StringBuilder s=new StringBuilder("RESOCONTO UNIFICATO SUPERENALOTTO\nI tre motori sono diagnostici, non predittori certi.\n\nINCROCIO POST-SPIA + CICLOMETRIA + STATISTICA\n");agree.entrySet().stream().sorted(Map.Entry.<Integer,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey())).limit(10).forEach(e->s.append(fmt(e.getKey())).append(" · concordanza ").append(e.getValue()).append(" motori\n"));s.append("\n").append(a).append("\n\n").append(b).append("\n\n").append(c);return s.toString();}
    private static Set<Integer> topFromReportNumbers(String report,int limit){Set<Integer> out=new LinkedHashSet<>();for(String line:report.split("\\r?\\n|\\r")){java.util.regex.Matcher m=java.util.regex.Pattern.compile("^\\s*(\\d{2})\\s+(?:freq=|· distanza|score)").matcher(line);if(m.find())out.add(Integer.parseInt(m.group(1)));if(out.size()>=limit)break;}return out;}
    private static String backtestReport(List<Draw>d,int spia){int min=120;if(d.size()<=min+1)throw new IllegalArgumentException("Per il backtest walk-forward servono almeno 122 estrazioni nel periodo.");List<Integer> hits=new ArrayList<>();for(int i=min;i<d.size()-1;i++)if(d.get(i).has(spia)){List<Draw> history=d.subList(0,i);String report=forecastReport(history,spia,false);Set<Integer> top=new HashSet<>();java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d{2})  score",java.util.regex.Pattern.MULTILINE).matcher(report);while(m.find()&&top.size()<6)top.add(Integer.parseInt(m.group(1)));if(top.isEmpty())continue;int h=0;for(int n:d.get(i+1).numbers)if(top.contains(n))h++;hits.add(h);}if(hits.isEmpty())return "Nessun evento spia utile per il backtest.";double avg=hits.stream().mapToInt(Integer::intValue).average().orElse(0);int[] dist=new int[7];for(int h:hits)dist[h]++;return String.format(Locale.ROOT,"BACKTEST WALK-FORWARD · TOP 6 contro estrazione successiva%nEventi valutati: %d%nHit medi: %.3f su 6%nRiferimento casuale teorico: %.3f%n",hits.size(),avg,6.0*6/90)+"Distribuzione hit:\n"+java.util.stream.IntStream.rangeClosed(0,6).mapToObj(h->h+ " hit: "+dist[h]).collect(Collectors.joining("\n"))+"\n\nIl test usa solo lo storico precedente a ciascun evento. Non garantisce risultati futuri.";}

    /**
     * Verifica sadica con holdout: per ogni evento la previsione usa soltanto lo storico precedente;
     * il 70% degli eventi validi è usato per la valutazione iniziale e il 30% finale resta holdout.
     */
    private static String sadicaHoldoutReport(List<Draw> d,int spia,int k) {
        int minimum=120;
        if(d.size()<minimum+k+8) throw new IllegalArgumentException("Per la verifica sadica servono almeno "+(minimum+k+8)+" estrazioni nel periodo.");
        List<Integer> eligible=new ArrayList<>();
        for(int i=minimum;i+k<d.size();i++) if(d.get(i).has(spia)) eligible.add(i);
        if(eligible.size()>60){List<Integer> sampled=new ArrayList<>();double step=eligible.size()/60.0;for(int j=0;j<60;j++)sampled.add(eligible.get((int)(j*step)));eligible=sampled;}
        List<Integer> modelHits=new ArrayList<>(),randomHits=new ArrayList<>();
        Random rng=new Random(71201L+spia);
        List<Integer> pool=java.util.stream.IntStream.rangeClosed(1,90).filter(n->n!=spia).boxed().collect(Collectors.toList());
        for(int i:eligible){
            List<Draw> history=d.subList(0,i);
            String report;try{report=forecastReport(history,spia,false);}catch(Exception ex){continue;}
            Set<Integer> top=new LinkedHashSet<>();
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d{2})  score",java.util.regex.Pattern.MULTILINE).matcher(report);
            while(m.find()&&top.size()<6){int n=Integer.parseInt(m.group(1));if(n!=spia)top.add(n);}
            if(top.size()<6)continue;
            Set<Integer> actual=new HashSet<>();for(int j=i+1;j<=i+k;j++)for(int n:d.get(j).numbers)actual.add(n);
            List<Integer> shuffled=new ArrayList<>(pool);Collections.shuffle(shuffled,rng);Set<Integer> random=new HashSet<>(shuffled.subList(0,6));
            modelHits.add(intersectionSize(top,actual));randomHits.add(intersectionSize(random,actual));
        }
        int n=modelHits.size();if(n<8)return "VERIFICA SADICA + HOLDOUT\nEventi validi: "+n+"\nDati insufficienti: servono almeno 8 eventi validi per distinguere valutazione e holdout.";
        int split=Math.max(4,Math.min(n-4,(int)Math.floor(n*.70)));
        List<Double> trainDiff=new ArrayList<>(),holdDiff=new ArrayList<>();
        for(int i=0;i<n;i++){double delta=modelHits.get(i)-randomHits.get(i);(i<split?trainDiff:holdDiff).add(delta);}
        List<Integer> trainModel=modelHits.subList(0,split),trainRandom=randomHits.subList(0,split);
        List<Integer> holdModel=modelHits.subList(split,n),holdRandom=randomHits.subList(split,n);
        double trainDelta=avg(trainModel)-avg(trainRandom),holdDelta=avg(holdModel)-avg(holdRandom);
        double[] ci=bootstrapCi(holdDiff,2000,71203L);double p=pairedRandomizationP(holdDiff,71204L,4000);
        double expected=6.0*(1.0-Math.pow(84.0/90.0,k));
        String verdict=holdDelta>0&&p<0.05?"SEGNALE INTERESSANTE DA RIVERIFICARE":"NON DIMOSTRATO SULL'HOLDOUT";
        return String.format(Locale.ROOT,"VERIFICA SADICA SUPERENALOTTO · WALK-FORWARD + HOLDOUT%nSpia: %02d · colpi successivi: %d%nEventi validi appaiati: %d%n%nVALUTAZIONE INIZIALE (70%%) · eventi %d%nModello TOP 6: %.3f hit medi nell'unione dei colpi%nCasuale TOP 6: %.3f%nDelta modello − casuale: %.3f%n%nHOLDOUT FINALE (30%%) · eventi %d%nModello TOP 6: %.3f hit medi nell'unione dei colpi%nCasuale TOP 6: %.3f%nRiferimento teorico casuale per 6 numeri: %.3f%nDelta modello − casuale: %.3f%nIC bootstrap 95%% del delta holdout: [%.3f, %.3f]%nP-value randomizzazione appaiato holdout: %.4f%nVERDETTO: %s%n%nOgni previsione è stata costruita usando solo estrazioni precedenti all'evento. L'holdout è una verifica esplorativa; eventi sovrapposti e scelta della spia limitano l'interpretazione statistica. Non dimostra un vantaggio futuro.",spia,k,n,split,avg(trainModel),avg(trainRandom),trainDelta,n-split,avg(holdModel),avg(holdRandom),expected,holdDelta,ci[0],ci[1],p,verdict);
    }

    /** Walk-forward ciclometrico ispirato al main.py: ogni previsione usa solo lo storico precedente. */
    private static String cyclometryBacktestReport(List<Draw> d,int spia,int k) {
        List<Integer> eligible=new ArrayList<>();
        for(int i=20;i<d.size()-k;i++) if(d.get(i).has(spia)) eligible.add(i);
        if(eligible.size()>40){List<Integer> sample=new ArrayList<>();double step=eligible.size()/40.0;for(int j=0;j<40;j++)sample.add(eligible.get((int)(j*step)));eligible=sample;}
        List<Integer> modelHits=new ArrayList<>(),randomHits=new ArrayList<>();
        Random rng=new Random(45601L);List<Integer> pool=java.util.stream.IntStream.rangeClosed(1,90).filter(n->n!=spia).boxed().collect(Collectors.toList());
        for(int i:eligible){
            List<Draw> history=d.subList(0,i);
            String report;
            try{report=cycloReport(history,spia,k);}catch(Exception ex){continue;}
            java.util.regex.Matcher evm=java.util.regex.Pattern.compile("eventi\\s+(\\d+)").matcher(report);
            if(!evm.find()||Integer.parseInt(evm.group(1))<3)continue;
            Set<Integer> top=topFromReportNumbers(report,5);if(top.size()<5)continue;
            Set<Integer> actual=new HashSet<>();for(int j=i+1;j<=i+k;j++)for(int n:d.get(j).numbers)actual.add(n);
            List<Integer> shuffled=new ArrayList<>(pool);Collections.shuffle(shuffled,rng);Set<Integer> random=new HashSet<>(shuffled.subList(0,5));
            modelHits.add(intersectionSize(top,actual));randomHits.add(intersectionSize(random,actual));
        }
        if(modelHits.isEmpty())return "Dati insufficienti per il backtest ciclometrico: servono almeno 3 cicli storici validi per evento e un periodo futuro completo.";
        double model=avg(modelHits),random=avg(randomHits),expected=5.0*(1.0-Math.pow(84.0/90.0,k));
        List<Double> diffs=new ArrayList<>();for(int i=0;i<modelHits.size();i++)diffs.add((double)(modelHits.get(i)-randomHits.get(i)));
        double[] ci=bootstrapCi(diffs,2000,45601L);double p=pairedRandomizationP(diffs,45602L,2000);
        int maxModel=modelHits.stream().mapToInt(Integer::intValue).max().orElse(0),maxRandom=randomHits.stream().mapToInt(Integer::intValue).max().orElse(0);
        return String.format(Locale.ROOT,"BACKTEST CICLOMETRIA vs CASUALE · walk-forward senza leakage%nEventi valutati: %d%nTOP 5 ciclometrico: %.3f hit medi nei successivi %d colpi%nTOP 5 casuale: %.3f hit medi nei successivi %d colpi%nRiferimento teorico casuale (unione dei colpi): %.3f%nDelta modello-casuale: %.3f%nIC bootstrap 95%%: [%.3f, %.3f]%nP-value randomizzazione appaiato: %.4f%nMassimo hit modello/casuale: %d / %d%n\nIl test misura solo lo storico scelto; non dimostra capacità predittiva futura.",modelHits.size(),model,k,random,k,expected,model-random,ci[0],ci[1],p,maxModel,maxRandom);
    }

    private static String comparativeBacktestReport(List<Draw> d,int spia,int k){
        int minimum=120;
        if(d.size()<=minimum+1) throw new IllegalArgumentException("Per il confronto walk-forward servono almeno 122 estrazioni nel periodo.");
        List<Integer> baseHits=new ArrayList<>(),modelHits=new ArrayList<>(),randomHits=new ArrayList<>();
        Random rng=new Random(2609L); List<Integer> eligible=new ArrayList<>();
        for(int i=minimum;i<d.size()-1;i++) if(d.get(i).has(spia)) eligible.add(i);
        if(eligible.size()>80){List<Integer> sampled=new ArrayList<>();double step=eligible.size()/80.0;for(int j=0;j<80;j++)sampled.add(eligible.get((int)(j*step)));eligible=sampled;}
        for(int i:eligible){List<Draw> history=d.subList(0,i);Set<Integer> base=topFromReportNumbers(postReport(history,spia,Math.min(9,Math.max(1,k))),6);Set<Integer> model=new LinkedHashSet<>();java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d{2})  score",java.util.regex.Pattern.MULTILINE).matcher(forecastReport(history,spia,false));while(m.find()&&model.size()<6)model.add(Integer.parseInt(m.group(1)));if(base.size()<6||model.size()<6)continue;Set<Integer> random=new HashSet<>();while(random.size()<6)random.add(1+rng.nextInt(90));Set<Integer> actual=Arrays.stream(d.get(i+1).numbers).boxed().collect(Collectors.toSet());baseHits.add(intersectionSize(base,actual));modelHits.add(intersectionSize(model,actual));randomHits.add(intersectionSize(random,actual));}
        if(modelHits.isEmpty())return "Nessun evento valido per il confronto walk-forward.";
        double mb=avg(baseHits),mm=avg(modelHits),mr=avg(randomHits);
        List<Double> diffRandom=new ArrayList<>(),diffBase=new ArrayList<>();
        for(int i=0;i<modelHits.size();i++){diffRandom.add((double)(modelHits.get(i)-randomHits.get(i)));diffBase.add((double)(modelHits.get(i)-baseHits.get(i)));}
        double meanRandom=avgD(diffRandom),meanBase=avgD(diffBase);
        double[] ciRandom=bootstrapCi(diffRandom,2000,2609L),ciBase=bootstrapCi(diffBase,2000,2611L);
        double pRandom=pairedRandomizationP(diffRandom,2610L,2000),pBase=pairedRandomizationP(diffBase,2612L,2000);
        return String.format(Locale.ROOT,"CONFRONTO WALK-FORWARD · nessun dato futuro usato per costruire la previsione%nEventi valutati appaiati: %d%nMedia hit TOP 6 post-spia: %.3f / 6%nMedia hit TOP 6 statistica: %.3f / 6%nMedia hit sestina casuale: %.3f / 6%nRiferimento teorico casuale: %.3f / 6%n\nSTATISTICA − CASUALE: delta %.3f · IC bootstrap 95%% [%.3f, %.3f] · p randomizzazione %.4f%nSTATISTICA − POST-SPIA: delta %.3f · IC bootstrap 95%% [%.3f, %.3f] · p randomizzazione %.4f%n\nIl confronto è esplorativo: dipendenze tra eventi, scelta della spia e molte analisi possibili limitano l'interpretazione dei p-value. Non implica che il modello migliori le probabilità future.",modelHits.size(),mb,mm,mr,6.0*6/90,meanRandom,ciRandom[0],ciRandom[1],pRandom,meanBase,ciBase[0],ciBase[1],pBase);
    }
    private static int intersectionSize(Set<Integer> a,Set<Integer> b){int n=0;for(int x:a)if(b.contains(x))n++;return n;}
    private static double avgD(List<Double> v){return v==null||v.isEmpty()?0:v.stream().mapToDouble(Double::doubleValue).average().orElse(0);}
    private static double pairedRandomizationP(List<Double> differences,long seed,int reps){if(differences.isEmpty())return 1;double observed=avgD(differences);Random r=new Random(seed);int extreme=0;for(int b=0;b<reps;b++){double sum=0;for(double v:differences)sum+=(r.nextBoolean()?v:-v);if(sum/differences.size()>=observed)extreme++;}return (extreme+1.0)/(reps+1.0);}
    private static double[] bootstrapCi(List<Double> values,int reps,long seed){if(values.isEmpty())return new double[]{0,0};Random r=new Random(seed);double[] means=new double[reps];for(int i=0;i<reps;i++){double sum=0;for(int j=0;j<values.size();j++)sum+=values.get(r.nextInt(values.size()));means[i]=sum/values.size();}Arrays.sort(means);return new double[]{means[(int)(.025*(reps-1))],means[(int)(.975*(reps-1))]};}

    private static double avg(List<Integer> v){return v==null||v.isEmpty()?0:v.stream().mapToInt(Integer::intValue).average().orElse(0);}
    private static double[] norm(Map<Integer,Integer> m){double[] x=new double[91];int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;for(int n=1;n<=90;n++){int v=m.getOrDefault(n,0);lo=Math.min(lo,v);hi=Math.max(hi,v);}if(lo==Integer.MAX_VALUE){lo=hi=0;}for(int n=1;n<=90;n++)x[n]=hi==lo?.5:(m.getOrDefault(n,0)-lo)/(double)(hi-lo);return x;}
    private static String fmt(int n){return String.format(Locale.ROOT,"%02d",n);}
    private static void combine(List<Integer> pool,int idx,int remaining,List<Integer> cur,List<List<Integer>> out){if(remaining==0){out.add(new ArrayList<>(cur));return;}for(int i=idx;i<=pool.size()-remaining;i++){cur.add(pool.get(i));combine(pool,i+1,remaining-1,cur,out);cur.remove(cur.size()-1);}}
    static List<Draw> sorted(Collection<Draw> input){Map<String,Draw> unique=new LinkedHashMap<>();for(Draw d:input)unique.put(d.key(),d);List<Draw> out=new ArrayList<>(unique.values());out.sort(Comparator.comparing(x->x.date));return out;}
    public static List<Draw> parseCsv(String content) throws Exception {
        return parseCsvWithAudit(content).draws;
    }

    public static ImportCsv parseCsvWithAudit(String content) throws Exception {
        String[] lines = content.replace("\uFEFF", "").split("\\r?\\n|\\r");
        if (lines.length < 2) throw new IOException("CSV vuoto o senza righe dati.");
        char delimiter = lines[0].contains(";") ? ';' : lines[0].contains("\t") ? '\t' : ',';
        List<String> header = split(lines[0], delimiter).stream().map(SuperEnalottoCore::norm).collect(Collectors.toList());
        int dateIndex = find(header, "DATA", "DATE");
        int contestIndex = find(header, "CONCORSO", "CONCORSO N", "CONCORSO NUMERO", "NCONCORSO");
        int[] numberIndexes = new int[6];
        for (int i = 0; i < numberIndexes.length; i++) {
            numberIndexes[i] = find(header, "N" + (i + 1), "NUMERO" + (i + 1), "NUM" + (i + 1));
        }
        int jollyIndex = find(header, "JOLLY");
        int superstarIndex = find(header, "SUPERSTAR");
        if (dateIndex < 0 || Arrays.stream(numberIndexes).anyMatch(x -> x < 0)) {
            throw new IOException("Formato CSV non riconosciuto: servono DATA e N1,N2,N3,N4,N5,N6.");
        }

        Map<LocalDate, Draw> unique = new LinkedHashMap<>();
        int valid = 0, duplicates = 0, invalid = 0;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            try {
                List<String> row = split(lines[i], delimiter);
                LocalDate date = parseDate(value(row, dateIndex));
                int[] numbers = new int[6];
                Set<Integer> seen = new HashSet<>();
                for (int j = 0; j < numbers.length; j++) {
                    numbers[j] = (int) Double.parseDouble(value(row, numberIndexes[j]).replace(',', '.'));
                    if (numbers[j] < 1 || numbers[j] > 90 || !seen.add(numbers[j])) {
                        throw new IllegalArgumentException("Sestina non valida");
                    }
                }
                valid++;
                if (unique.containsKey(date)) duplicates++;
                unique.put(date, new Draw(date, value(row, contestIndex), numbers,
                        value(row, jollyIndex), value(row, superstarIndex)));
            } catch (Exception ignored) {
                invalid++;
            }
        }
        return new ImportCsv(sorted(unique.values()), valid, duplicates, invalid);
    }
    private static String value(List<String> row,int i){return i<0||i>=row.size()?"":row.get(i).trim();}
    private static int find(List<String> h,String... keys){for(String k:keys){String n=norm(k);for(int i=0;i<h.size();i++)if(h.get(i).equals(n))return i;}return -1;}
    private static String norm(String s){return s==null?"":s.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]","");}
    private static LocalDate parseDate(String s){for(DateTimeFormatter f:Arrays.asList(DateTimeFormatter.ofPattern("d/M/uuuu"),DateTimeFormatter.ofPattern("dd-MM-uuuu"),DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("d-M-uuuu")))try{return LocalDate.parse(s.trim(),f);}catch(Exception ignored){}throw new IllegalArgumentException("Data non valida: "+s);}
    private static List<String> split(String line,char delimiter){List<String> out=new ArrayList<>();StringBuilder cur=new StringBuilder();boolean quoted=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){cur.append('"');i++;}else quoted=!quoted;}else if(c==delimiter&&!quoted){out.add(cur.toString());cur.setLength(0);}else cur.append(c);}out.add(cur.toString());return out;}

    // ------------------------------------------------------------------ interfaccia verso l'app

    /** Le tecniche disponibili: chiave, etichetta. */
    public static final String[][] TECNICHE = {
            {"post", "Analisi post-spia"},
            {"forecast", "Previsione statistica"},
            {"cyclo", "Ciclometria spia"},
            {"unified", "Resoconto unificato"},
            {"backtest", "Backtest walk-forward"},
            {"compare", "Confronto modelli"},
            {"cyclobacktest", "Backtest ciclometria vs casuale"},
            {"holdout", "Verifica sadica + holdout"},
            {"scanner", "🔎 Scanner di spie (classifica e verifica)"}
    };

    public static List<Draw> filtra(List<Draw> tutte, int daAnno, int aAnno) {
        List<Draw> out = new ArrayList<>();
        for (Draw d : tutte) if (d.date.getYear() >= daAnno && d.date.getYear() <= aAnno) out.add(d);
        return out;
    }

    /** Rapporto di una tecnica sul periodo indicato (può lanciare IllegalArgumentException con il motivo). */
    public static String rapporto(String modo, List<Draw> periodo, int spia, int colpi) {
        if (periodo == null || periodo.isEmpty()) throw new IllegalArgumentException("Nessuna estrazione nel periodo selezionato.");
        switch (modo) {
            case "post": return postReport(periodo, spia, colpi);
            case "forecast": return forecastReport(periodo, spia);
            case "cyclo": return cycloReport(periodo, spia, colpi);
            case "backtest": return backtestReport(periodo, spia);
            case "compare": return comparativeBacktestReport(periodo, spia, colpi);
            case "cyclobacktest": return cyclometryBacktestReport(periodo, spia, colpi);
            case "holdout": return sadicaHoldoutReport(periodo, spia, colpi);
            case "scanner": return scansiona(periodo, colpi).report(colpi);
            default: return unifiedReport(periodo, spia, colpi);
        }
    }

    /** Evidenze archivio: spia verde, candidati blu e riscontri nei colpi successivi rossi. */
    public static final class Evidenza {
        public final Set<Integer> candidati = new LinkedHashSet<>();
        public final Set<Integer> candidatiBlu = new LinkedHashSet<>();
        public final Set<String> chiaviSpia = new HashSet<>();
        public final Set<String> chiaviRisultato = new HashSet<>();
    }

    private static final int SCANNER_MIN_CICLI_STORICI = 10;
    private static final int SCANNER_NUMERI_CANDIDATI = 6;

    public static final class RigaScanner {
        public int spia;
        public int cicli;
        public int prove;
        public int successi;
        public double percentuale;
        public double zCaso;
        public final List<Integer> candidati = new ArrayList<>();
    }

    public static final class RisultatoScanner {
        public final List<RigaScanner> righe = new ArrayList<>();
        public int spieValutate;
        public int spieSaltate;
        public double successoCaso;
        public LocalDate dal;
        public LocalDate al;

        public RigaScanner migliore() {
            return righe.isEmpty() ? null : righe.get(0);
        }

        public String report(int colpi) {
            StringBuilder sb = new StringBuilder("SCANNER DI SPIE · SUPERENALOTTO\n");
            sb.append("Periodo: ").append(dal).append(" – ").append(al)
                    .append("\nColpi successivi: ").append(colpi)
                    .append(" · numeri candidati per ciclo: ").append(SCANNER_NUMERI_CANDIDATI)
                    .append("\nSpie valutate: ").append(spieValutate).append(" su 90 (saltate per pochi cicli: ")
                    .append(spieSaltate).append(")\n");
            sb.append(String.format(Locale.ROOT,
                    "Probabilità di almeno un candidato in %d estrazione/i per puro caso: %.1f%%\n",
                    colpi, successoCaso * 100));
            if (righe.isEmpty()) {
                sb.append("\nArchivio insufficiente: servono almeno ")
                        .append(SCANNER_MIN_CICLI_STORICI + 1)
                        .append(" cicli completi per una spia.");
                return sb.toString();
            }
            sb.append("\nCLASSIFICA WALK-FORWARD (candidate scelti solo con cicli precedenti)\n")
                    .append(String.format(Locale.ROOT, "%3s %-5s %7s %7s %8s %8s  %s%n",
                            "#", "Spia", "Cicli", "Prove", "Successi", "z caso", "Candidati storici"));
            for (int i = 0; i < Math.min(20, righe.size()); i++) {
                RigaScanner r = righe.get(i);
                String candidati = r.candidati.stream().map(SuperEnalottoCore::fmt)
                        .collect(Collectors.joining("-"));
                sb.append(String.format(Locale.ROOT, "%3d %02d %7d %7d %7.1f%% %+8.2f  %s%n",
                        i + 1, r.spia, r.cicli, r.prove, r.percentuale, r.zCaso, candidati));
            }
            sb.append("\nIl punteggio confronta i successi osservati con il puro caso; non predice né aumenta ")
                    .append("la probabilità delle estrazioni future.");
            return sb.toString();
        }
    }

    /** Scansiona tutte le spie con valutazione walk-forward e candidati ricavati solo dai cicli precedenti. */
    public static RisultatoScanner scansiona(List<Draw> periodo, int colpi) {
        if (periodo == null || periodo.isEmpty()) {
            throw new IllegalArgumentException("Nessuna estrazione nel periodo selezionato.");
        }
        if (colpi < 1 || colpi > 50) {
            throw new IllegalArgumentException("I colpi devono essere tra 1 e 50.");
        }

        List<Draw> draws = sorted(periodo);
        RisultatoScanner result = new RisultatoScanner();
        result.dal = draws.get(0).date;
        result.al = draws.get(draws.size() - 1).date;
        double pUnaEstrazione = 1.0;
        for (int i = 0; i < SCANNER_NUMERI_CANDIDATI; i++) {
            pUnaEstrazione *= (84.0 - i) / (90.0 - i);
        }
        result.successoCaso = 1.0 - Math.pow(pUnaEstrazione, colpi);

        for (int spia = 1; spia <= 90; spia++) {
            int[] frequenze = new int[91];
            RigaScanner riga = new RigaScanner();
            for (int i = 0; i < draws.size(); i++) {
                if (!draws.get(i).has(spia)) continue;
                if (i + colpi >= draws.size()) continue;

                riga.cicli++;
                if (riga.cicli > SCANNER_MIN_CICLI_STORICI) {
                    List<Integer> candidati = primiCandidati(frequenze, spia);
                    riga.prove++;
                    boolean trovato = false;
                    for (int j = i + 1; j <= i + colpi && !trovato; j++) {
                        for (int n : draws.get(j).numbers) {
                            if (candidati.contains(n)) {
                                trovato = true;
                                break;
                            }
                        }
                    }
                    if (trovato) riga.successi++;
                }

                for (int j = i + 1; j <= i + colpi; j++) {
                    for (int n : draws.get(j).numbers) frequenze[n]++;
                }
            }

            if (riga.prove == 0) {
                result.spieSaltate++;
                continue;
            }
            riga.spia = spia;
            riga.percentuale = riga.successi * 100.0 / riga.prove;
            double varianza = riga.prove * result.successoCaso * (1.0 - result.successoCaso);
            riga.zCaso = varianza > 0
                    ? (riga.successi - riga.prove * result.successoCaso) / Math.sqrt(varianza)
                    : 0;
            riga.candidati.addAll(primiCandidati(frequenze, spia));
            result.righe.add(riga);
        }

        result.spieValutate = result.righe.size();
        result.righe.sort(Comparator.comparingDouble((RigaScanner r) -> r.zCaso).reversed()
                .thenComparing(Comparator.comparingDouble((RigaScanner r) -> r.percentuale).reversed())
                .thenComparingInt(r -> r.spia));
        return result;
    }

    public static final class ConfrontoScanner {
        public final RisultatoScanner addestramento;
        public final String report;
        public final int inizioTest;

        private ConfrontoScanner(RisultatoScanner training, String report, int testStart) {
            this.addestramento = training;
            this.report = report;
            this.inizioTest = testStart;
        }
    }

    /** Seleziona le spie nella prima parte e misura i candidati fissi nella parte successiva mai vista. */
    public static ConfrontoScanner confrontaScanner(List<Draw> periodo, int colpi) {
        if (periodo == null || periodo.size() < 30) {
            throw new IllegalArgumentException("Servono almeno 30 estrazioni per separare addestramento e test.");
        }
        List<Draw> ordered = sorted(periodo);
        int split = Math.max(1, Math.min(ordered.size() - 1, (int) Math.floor(ordered.size() * 0.7)));
        List<Draw> training = new ArrayList<>(ordered.subList(0, split));
        List<Draw> test = new ArrayList<>(ordered.subList(split, ordered.size()));
        RisultatoScanner scan = scansiona(training, colpi);
        StringBuilder report = new StringBuilder("SCANNER DI SPIE · TEST FUORI CAMPIONE\n")
                .append("Addestramento: ").append(training.get(0).date).append(" – ").append(training.get(training.size() - 1).date)
                .append("\nTest mai usato per scegliere le spie: ").append(test.get(0).date)
                .append(" – ").append(test.get(test.size() - 1).date)
                .append("\nColpi successivi: ").append(colpi)
                .append("\n\nLe prime spie sono selezionate solo sull'addestramento; nel test i candidati restano fissi.\n");
        if (scan.righe.isEmpty()) {
            report.append("\nNessuna spia ha abbastanza cicli completi nella parte di addestramento.");
            return new ConfrontoScanner(scan, report.toString(), split);
        }
        report.append(String.format(Locale.ROOT, "%-5s %-8s %-8s %-8s %-8s  %s%n",
                "Spia", "Cicli", "Test", "Successi", "Test %", "Candidati scelti in addestramento"));
        for (int i = 0; i < Math.min(10, scan.righe.size()); i++) {
            RigaScanner r = scan.righe.get(i);
            int prove = 0, successi = 0;
            for (int start = 0; start + colpi < test.size(); start++) {
                if (!test.get(start).has(r.spia)) continue;
                prove++;
                boolean hit = false;
                for (int j = start + 1; j <= start + colpi && !hit; j++) {
                    for (int n : test.get(j).numbers) {
                        if (r.candidati.contains(n)) {
                            hit = true;
                            break;
                        }
                    }
                }
                if (hit) successi++;
            }
            String testPct = prove == 0 ? "n.d." : String.format(Locale.ROOT, "%.1f%%", successi * 100.0 / prove);
            String numeri = r.candidati.stream().map(SuperEnalottoCore::fmt).collect(Collectors.joining("-"));
            report.append(String.format(Locale.ROOT, "%-5s %-8d %-8d %-8d %-8s  %s%n",
                    fmt(r.spia), r.cicli, prove, successi, testPct, numeri));
        }
        report.append(String.format(Locale.ROOT,
                "\nPuro caso per almeno un candidato in %d colpo/i: %.1f%%.", colpi, scan.successoCaso * 100))
                .append(" Confronta il test solo se ci sono abbastanza cicli.")
                .append("\nUn test con pochi cicli è molto incerto; i risultati storici non prevedono le estrazioni.");
        return new ConfrontoScanner(scan, report.toString(), split);
    }

    private static List<Integer> primiCandidati(int[] frequenze, int spia) {
        return java.util.stream.IntStream.rangeClosed(1, 90)
                .filter(n -> n != spia)
                .boxed()
                .sorted(Comparator.<Integer>comparingInt(n -> -frequenze[n]).thenComparingInt(n -> n))
                .limit(SCANNER_NUMERI_CANDIDATI)
                .collect(Collectors.toList());
    }

    public static Evidenza evidenzaScanner(List<Draw> periodo, int spia, int colpi, List<Integer> candidati) {
        Evidenza ev = new Evidenza();
        if (periodo == null || periodo.isEmpty() || spia < 1 || spia > 90 || colpi < 1) return ev;
        ev.candidati.addAll(candidati);
        List<Draw> draws = sorted(periodo);
        for (int i = 0; i < draws.size(); i++) {
            if (!draws.get(i).has(spia)) continue;
            ev.chiaviSpia.add(draws.get(i).key());
            int disponibili = Math.min(colpi, draws.size() - i - 1);
            for (int j = i + 1; j <= i + disponibili; j++) {
                ev.chiaviRisultato.add(draws.get(j).key());
            }
        }
        return ev;
    }

    public static Evidenza evidenza(List<Draw> period, int spia, int k, String mode) {
        Evidenza ev = new Evidenza();
        Map<Integer,Integer> freq=new HashMap<>(),cycles=new HashMap<>(); Map<Integer,List<Integer>> pos=new HashMap<>(); int events=0;
        for(int i=0;i<period.size();i++) if(period.get(i).has(spia)) {
            ev.chiaviSpia.add(period.get(i).key());
            int available=Math.min(k,period.size()-i-1);
            for(int j=i+1;j<=i+available;j++) ev.chiaviRisultato.add(period.get(j).key());
            if(available<k) continue; // ciclo aperto: si colora, ma non entra nei punteggi storici
            events++; Set<Integer> seen=new HashSet<>(); Map<Integer,Integer> firstPosition=new HashMap<>();
            for(int j=i+1;j<=i+k;j++){Draw d=period.get(j);for(int n:d.numbers){freq.merge(n,1,Integer::sum);seen.add(n);if(!firstPosition.containsKey(n))firstPosition.put(n,j-i);}}
            for(int n:seen){cycles.merge(n,1,Integer::sum);List<Integer> l=pos.get(n);if(l==null){l=new ArrayList<>();pos.put(n,l);}l.add(firstPosition.get(n));}
        }
        if(mode.equals("forecast") || mode.equals("backtest") || mode.equals("compare") || mode.equals("holdout")) {
            ev.candidati.addAll(topFromReportNumbers(forecastReport(period,spia),15));
        } else if(mode.equals("cyclo") || mode.equals("cyclobacktest")) {
            ev.candidati.addAll(topFromReportNumbers(cycloReport(period,spia,k),15));
        } else if(mode.equals("unified")) {
            ev.candidati.addAll(topFromReportNumbers(postReport(period,spia,k),10));
            ev.candidati.addAll(topFromReportNumbers(cycloReport(period,spia,k),10));
            ev.candidati.addAll(topFromReportNumbers(forecastReport(period,spia),15));
        } else if(events>0) {
            ev.candidati.addAll(rank(freq,cycles,pos,events,k,spia).stream().limit(15).collect(Collectors.toList()));
        }
        return ev;
    }

    /** CSV dell'archivio (stesso formato della versione Linux): DATA;CONCORSO;N1..N6;JOLLY;SUPERSTAR. */
    public static String aCsv(Collection<Draw> data) {
        StringBuilder s = new StringBuilder("DATA;CONCORSO;N1;N2;N3;N4;N5;N6;JOLLY;SUPERSTAR\n");
        for (Draw d : sorted(data)) {
            s.append(d.date).append(';').append(pulisci(d.contest));
            for (int n : d.numbers) s.append(';').append(n);
            s.append(';').append(pulisci(d.jolly)).append(';').append(pulisci(d.superstar)).append('\n');
        }
        return s.toString();
    }

    private static String pulisci(String s) { return s == null ? "" : s.replace(';', ',').replace('\n', ' '); }

    /** Unisce le nuove estrazioni a quelle esistenti, senza doppioni (la stessa data viene sostituita). */
    public static List<Draw> unisci(List<Draw> esistenti, List<Draw> nuove) {
        Map<String, Draw> map = new LinkedHashMap<>();
        for (Draw d : esistenti) map.put(d.key(), d);
        for (Draw d : nuove) map.put(d.key(), d);
        return sorted(map.values());
    }

    public static String riepilogo(List<Draw> draws) {
        if (draws.isEmpty()) return "Archivio SuperEnalotto vuoto.";
        return "ARCHIVIO SUPERENALOTTO\nEstrazioni: " + draws.size() + "\nPrima: " + draws.get(0).date + "\nUltima: " + draws.get(draws.size() - 1).date;
    }

    public static final class ImportCsv {
        public final List<Draw> draws;
        public final int righeValide;
        public final int righeDuplicate;
        public final int righeNonValide;

        private ImportCsv(List<Draw> draws, int valid, int duplicate, int invalid) {
            this.draws = draws;
            this.righeValide = valid;
            this.righeDuplicate = duplicate;
            this.righeNonValide = invalid;
        }

        public String report() {
            return "Righe valide: " + righeValide + "\nDoppioni rimossi: " + righeDuplicate
                    + "\nRighe non riconosciute: " + righeNonValide;
        }
    }

    /** Controlli non distruttivi: le date con intervalli lunghi sono segnalate come possibili lacune. */
    public static String diagnosticaArchivio(List<Draw> data) {
        if (data == null || data.isEmpty()) return "Archivio vuoto: nessun controllo possibile.";
        List<Draw> ordered = sorted(data);
        Map<String, Integer> concorsi = new HashMap<>();
        for (Draw d : ordered) {
            String contest = d.contest.trim();
            if (!contest.isEmpty()) concorsi.merge(d.date.getYear() + "/" + contest, 1, Integer::sum);
        }
        List<String> duplicati = concorsi.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> e.getKey() + " (" + e.getValue() + " volte)")
                .sorted()
                .collect(Collectors.toList());
        List<String> lacune = new ArrayList<>();
        for (int i = 1; i < ordered.size(); i++) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(ordered.get(i - 1).date, ordered.get(i).date);
            if (days > 10) {
                lacune.add(ordered.get(i - 1).date + " → " + ordered.get(i).date + " (" + days + " giorni)");
            }
        }
        Draw latest = ordered.get(ordered.size() - 1);
        StringBuilder report = new StringBuilder("CONTROLLO ARCHIVIO SUPERENALOTTO\n")
                .append("Estrazioni uniche: ").append(ordered.size())
                .append("\nUltima estrazione: ").append(latest.date)
                .append(latest.contest.trim().isEmpty() ? "" : " · concorso " + latest.contest)
                .append("\nDoppioni di numero concorso: ").append(duplicati.size());
        for (String duplicate : duplicati) report.append("\n  • ").append(duplicate);
        report.append("\nIntervalli oltre 10 giorni: ").append(lacune.size());
        for (String gap : lacune) report.append("\n  • ").append(gap).append(" (verifica se manca un'estrazione)");
        if (duplicati.isEmpty() && lacune.isEmpty()) report.append("\nNessuna anomalia rilevata.");
        report.append("\n\nGli intervalli lunghi sono solo avvisi: il calendario delle estrazioni può variare.");
        return report.toString();
    }

    /** Numeri caldi e freddi: frequenza storica, ultime 100 estrazioni, ritardi e un test di uniformità (chi quadro). */
    public static String statisticheNumeri(List<Draw> draws) {
        if (draws == null || draws.isEmpty()) return "Archivio SuperEnalotto vuoto.";
        final int n = draws.size();
        final int[] freq = new int[91], rec = new int[91], ultima = new int[91];
        Arrays.fill(ultima, -1);
        final int finestra = Math.min(100, n);
        for (int i = 0; i < n; i++) {
            for (int x : draws.get(i).numbers) {
                freq[x]++;
                ultima[x] = i;
                if (i >= n - finestra) rec[x]++;
            }
        }
        final double atteso = n * 6.0 / 90.0;
        double chi = 0;
        for (int x = 1; x <= 90; x++) chi += (freq[x] - atteso) * (freq[x] - atteso) / atteso;
        double z = (chi - 89.0) / Math.sqrt(2 * 89.0);
        StringBuilder sb = new StringBuilder("NUMERI CALDI E FREDDI \u00B7 SUPERENALOTTO\nEstrazioni: " + n + " \u00B7 uscite attese per numero: "
                + String.format(Locale.ROOT, "%.1f", atteso) + "\n");
        elenco(sb, "PI\u00D9 FREQUENTI (storico)", freq, atteso, true);
        elenco(sb, "MENO FREQUENTI (storico)", freq, atteso, false);
        sb.append("\nPI\u00D9 USCITI NELLE ULTIME ").append(finestra).append(" ESTRAZIONI\n");
        Integer[] ordine = new Integer[90];
        for (int x = 0; x < 90; x++) ordine[x] = x + 1;
        Arrays.sort(ordine, (p, q) -> rec[q] != rec[p] ? Integer.compare(rec[q], rec[p]) : Integer.compare(p, q));
        for (int i = 0; i < 10; i++) sb.append(String.format(Locale.ROOT, "%02d  uscite %d (attese %.1f)%n", ordine[i], rec[ordine[i]], finestra * 6.0 / 90.0));
        sb.append("\nMAGGIOR RITARDO (estrazioni dall'ultima uscita)\n");
        final int[] rit = new int[91];
        for (int x = 1; x <= 90; x++) rit[x] = ultima[x] < 0 ? n : n - 1 - ultima[x];
        Arrays.sort(ordine, (p, q) -> rit[q] != rit[p] ? Integer.compare(rit[q], rit[p]) : Integer.compare(p, q));
        for (int i = 0; i < 10; i++) sb.append(String.format(Locale.ROOT, "%02d  ritardo %d%n", ordine[i], rit[ordine[i]]));
        sb.append(String.format(Locale.ROOT, "%nTEST DI UNIFORMIT\u00C0: chi quadro %.1f (atteso circa 89 \u00B1 13), z = %+.2f%n", chi, z));
        sb.append(Math.abs(z) < 2
                ? "Le frequenze sono compatibili con un'estrazione casuale: i numeri \"caldi\" e \"freddi\" sono normali oscillazioni.\n"
                : "Le frequenze si discostano dal caso pi\u00F9 del previsto: controlla che l'archivio non abbia errori o estrazioni mancanti.\n");
        sb.append("Nota: i ritardi e le frequenze storiche non cambiano la probabilit\u00E0 della prossima estrazione.");
        return sb.toString();
    }

    /** Frequenze e ritardi del numero Jolly o SuperStar (1-90), separati dai sei estratti principali. */
    public static String statisticheExtra(List<Draw> draws, boolean superstar) {
        String titolo = superstar ? "SUPERSTAR" : "JOLLY";
        if (draws == null || draws.isEmpty()) return "Archivio SuperEnalotto vuoto.";
        int[] frequenze = frequenze(draws, superstar ? 2 : 1);
        int validi = Arrays.stream(frequenze).sum();
        if (validi == 0) return "Nessun dato numerico valido per " + titolo + ".";
        int[] ultima = new int[91];
        Arrays.fill(ultima, -1);
        for (int i = 0; i < draws.size(); i++) {
            String raw = superstar ? draws.get(i).superstar : draws.get(i).jolly;
            int numero = numeroExtra(raw);
            if (numero < 1 || numero > 90) continue;
            ultima[numero] = i;
        }

        List<Integer> ordine = new ArrayList<>();
        for (int n = 1; n <= 90; n++) ordine.add(n);
        ordine.sort(Comparator.comparingInt((Integer n) -> -frequenze[n]).thenComparingInt(n -> n));
        StringBuilder sb = new StringBuilder("STATISTICHE ").append(titolo)
                .append("\nEstrazioni con dato valido: ").append(validi).append(" su ").append(draws.size())
                .append("\n\nPIÙ FREQUENTI\n");
        for (int i = 0; i < 10; i++) {
            int n = ordine.get(i);
            sb.append(String.format(Locale.ROOT, "%02d · uscite %d · ritardo %d%n",
                    n, frequenze[n], ultima[n] < 0 ? draws.size() : draws.size() - 1 - ultima[n]));
        }
        ordine.sort(Comparator.comparingInt((Integer n) -> ultima[n] < 0 ? draws.size() : draws.size() - 1 - ultima[n])
                .reversed().thenComparingInt(n -> n));
        sb.append("\nMAGGIOR RITARDO\n");
        for (int i = 0; i < 10; i++) {
            int n = ordine.get(i);
            sb.append(String.format(Locale.ROOT, "%02d · ritardo %d · uscite %d%n",
                    n, ultima[n] < 0 ? draws.size() : draws.size() - 1 - ultima[n], frequenze[n]));
        }
        sb.append("\nLe frequenze storiche non modificano la probabilità delle prossime estrazioni.");
        return sb.toString();
    }

    /** Frequenze indicizzate 1-90: modalità 0 = sestina, 1 = Jolly, 2 = SuperStar. */
    public static int[] frequenze(List<Draw> draws, int modalita) {
        int[] frequenze = new int[91];
        if (draws == null) return frequenze;
        for (Draw draw : draws) {
            if (modalita == 0) {
                for (int n : draw.numbers) if (n >= 1 && n <= 90) frequenze[n]++;
            } else if (modalita == 1 || modalita == 2) {
                int n = numeroExtra(modalita == 1 ? draw.jolly : draw.superstar);
                if (n >= 1 && n <= 90) frequenze[n]++;
            } else {
                throw new IllegalArgumentException("Tipo di numero non riconosciuto.");
            }
        }
        return frequenze;
    }

    private static int numeroExtra(String raw) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void elenco(StringBuilder sb, String titolo, int[] freq, double atteso, boolean alti) {
        sb.append("\n").append(titolo).append("\n");
        Integer[] ordine = new Integer[90];
        for (int x = 0; x < 90; x++) ordine[x] = x + 1;
        final int segno = alti ? 1 : -1;
        Arrays.sort(ordine, (p, q) -> freq[q] != freq[p] ? segno * Integer.compare(freq[q], freq[p]) : Integer.compare(p, q));
        for (int i = 0; i < 10; i++) {
            int x = ordine[i];
            sb.append(String.format(Locale.ROOT, "%02d  uscite %d (%+.1f%% rispetto all'atteso)%n", x, freq[x], (freq[x] - atteso) * 100.0 / atteso));
        }
    }
}
