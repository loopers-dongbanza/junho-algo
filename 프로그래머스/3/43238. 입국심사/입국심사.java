import java.util.*;
class Solution {
    public long solution(int n, int[] times) {
        
        Arrays.sort(times);
        long left = 1;
        // long right = times.length - 1;
        long right = (long) times[times.length - 1] * n; // 최악의 경우

        long answer = right;

        while(left<=right){
            long mid = (left + right) / 2;
            long people = 0;
            for(int time :times){
                people += mid / time;
                if(people >= n){
                    break;
                }
            }
            if(people >= n){
                answer = mid;
                right = mid - 1;
            } else{
                left = mid + 1;
            }
        }
        
        
        return answer;
    }
}