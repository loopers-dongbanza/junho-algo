class Solution {
    public int solution(int[][] sizes) {
        int maxWidth = 0;
        int maxHeight = 0;

        for (int[] size : sizes) {
            int longer = Math.max(size[0], size[1]);
            int shorter = Math.min(size[0], size[1]);

            maxWidth = Math.max(maxWidth, longer);
            maxHeight = Math.max(maxHeight, shorter);
        }

        return maxWidth * maxHeight;
    }
}