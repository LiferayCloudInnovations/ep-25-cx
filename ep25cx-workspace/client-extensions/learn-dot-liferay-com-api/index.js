const express = require('express');
const axios = require('axios');

const app = express();
const PORT = 3001;

app.get("/ready", (req, res) => {
	res.send('READY');
});


app.get('/suggestions', async (req, res) => {
  const searchTerm = req.query.search;

  if (!searchTerm) {
    return res.status(400).json({ error: "Missing 'search' query parameter" });
  }

  const data = JSON.stringify([
    {
      attributes: {
        includeAssetSearchSummary: true,
        includeAssetURL: true,
        sxpBlueprintId: 0
      },
      contributorName: "sxpBlueprint",
      displayGroupName: "Public Nav Search Recommendations",
      size: "3"
    }
  ]);

  const config = {
    method: 'post',
    maxBodyLength: Infinity,
    url: `https://learn.liferay.com/o/portal-search-rest/v1.0/suggestions?currentURL=https://learn.liferay.com/&destinationFriendlyURL=/searchasas&groupId=23484947&plid=4975&scope=this-site&search=${encodeURIComponent(searchTerm)}`,
    headers: {
      'Content-Type': 'application/json'
    },
    data: data
  };

  try {
    const response = await axios.request(config);
    res.json(response.data);
  } catch (error) {
    res.status(500).json({ error: error.response?.data || error.message });
  }
});

app.listen(PORT, () => {
  console.log(`API listening at http://localhost:${PORT}`);
});
