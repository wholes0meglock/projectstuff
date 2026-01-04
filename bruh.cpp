#include <bits/stdc++.h>
using namespace std;
#define int long long
void help() {
    int n,s;
    cin >> n >> s;
    vector<int> vec;
    map<int,int> m;
    for(int i = 0; i < n; i++)
    {
        int num;
        cin >> num;
        vec.push_back(num);
        m[num]++;
    }
    if(m[1] > s)
    {
        cout << -1 << endl;
        return;
    }

    vector<int> prefix(n);
    if(vec[0] == 1) prefix[0] = 1;
    else prefix[0] = 0;

    for(int i = 1; i < n; i++)
    {
        prefix[i] = prefix[i-1] + (vec[i] == 1);
    }

    vector<int> suffix(n);
    if(vec[n-1] == 1) suffix[n-1] = 1;
    else suffix[n-1] = 0;

    for(int i = n-2; i >= 0; i--)
    {
        suffix[i] = suffix[i+1] + (vec[i] == 1);
    }

    //st for prefix, en for suffix
    //start from both sides and check via m[1]


}

signed main()
{
    //Jesus saves.
    ios::sync_with_stdio(false);
    cin.tie(nullptr);
    int t;
    cin >> t;
    while(t--)
    help();
}








