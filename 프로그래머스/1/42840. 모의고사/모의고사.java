import java.util.*;

class Solution {
    public int[] solution(int[] answers) {
        int[] answer = {};
        
        int[] person1 = {1,2,3,4,5};
        int[] person2 = {2,1,2,3,2,4,2,5};
        int[] person3 = {3,3,1,1,2,2,4,4,5,5};
        
        int answer1 = 0;
        int answer2 = 0;
        int answer3 = 0;

        for(int i=0; i<answers.length; i++){
            if (answers[i] == person1[i % person1.length]) answer1++;
            if (answers[i] == person2[i % person2.length]) answer2++;
            if (answers[i] == person3[i % person3.length]) answer3++;
        }
        
        int[] score = {answer1, answer2, answer3};

        int max = score[0];
        for (int s : score) {
            if (s > max) max = s;
        }
        
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < score.length; i++) if (score[i] == max) res.add(i + 1);

        
        int[] arr = new int[res.size()]; 

        for (int i = 0; i < res.size(); i++) {
            arr[i] = res.get(i); 
        }


        return arr;  
    }
}