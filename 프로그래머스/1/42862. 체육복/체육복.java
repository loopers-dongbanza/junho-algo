import java.util.*;

class Solution {
    public int solution(int n, int[] lost, int[] reserve) {
        int answer = 0;
        HashMap<Integer, Integer> box = new HashMap<>();

        for (int i = 1; i <= n; i++) {
            box.put(i, 1);
        }

        for (int num : lost) {
            box.put(num, box.get(num) - 1);
        }

        for (int num : reserve) {
            box.put(num, box.get(num) + 1);
        }
        
        for (int i = 1; i <= n; i++) {
            if(box.get(i) == 0){
                if (i > 1 && box.get(i - 1) == 2) {
                    box.put(i-1,1);
                    box.put(i,1);
                } else if (i < n && box.get(i + 1) == 2) {
                    box.put(i + 1, 1);
                    box.put(i, 1);
                }
            }   
        }
        
        for (int i = 1; i <= n; i++) {
            if (box.get(i) > 0) answer++;
        }
        
        return answer;
    }
}