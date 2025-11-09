// 문제가 개편되었습니다. 이로 인해 함수 구성이나 테스트케이스가 변경되어, 과거의 코드는 동작하지 않을 수 있습니다.
// 새로운 함수 구성을 적용하려면 [코드 초기화] 버튼을 누르세요. 단, [코드 초기화] 버튼을 누르면 작성 중인 코드는 사라집니다.
class Solution {
    int answer = -1;

    public int solution(int N, int number) {
        dfs(N, 0, 0, number, "");
        return answer;
    }

    public void dfs(int n, int pos, int num, int number, String s) {
        if (pos > 8)
            return;
        if (num == number) {
            if (pos < answer || answer == -1) {
                System.out.println(s);
                answer = pos;
            }
            return;
        }
        int nn=0;
        for (int i = 0; i < 8; i++) {
            nn=nn*10+n;
            dfs(n, pos + 1+i, num + nn, number, s + "+");
            dfs(n, pos + 1+i, num - nn, number, s + "-");
            dfs(n, pos + 1+i, num * nn, number, s + "*");
            dfs(n, pos + 1+i, num / nn, number, s + "/");
        }
        // dfs(n,pos+1,num*10+n,number,s+"5");
    }
}