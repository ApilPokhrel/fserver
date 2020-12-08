let call = async (payload, cb) => {
  try {
    let { data } = await Axios({
      url: `${API_URL}/file/gateway`,
      method: "post",
      headers: {
        token: window.localStorage.getItem("token") || ""
      },
      data: {
        name: payload.file.name,
        exp: 2400000,
        replace: payload.replace
      }
    });


    if(data.multipart){
        return multipart(payload, data, cb);
    } else {
        cb(100);
        return raw(data);
    }
  } catch (err) {
    alert(err.message);
  }
};


let multipart = (payload, data, cb) => {
    let formData = new FormData();
    formData.append("file", payload.file);

    return new Promise((resolve, reject) => {
      Axios({
        method: "post",
        headers: {
          "Content-Type": "multipart/form-data",
          authorization: "Bearer " + data.token
        },
        data: formData,
        url: data.url,
        onUploadProgress: ev => {
          const progress = (ev.loaded / ev.total) * 100;
          cb(Math.round(progress));
        }
      })
        .then(resp => {
          resolve({ name: resp.data.name, url: "https://cdn.digherup.com/", from: "local" });
        })
        .catch(err => reject(err));
    });
}

let raw = (data) => {
    return new Promise((resolve, reject) => {
      Axios({
        method: data.method,
        headers: {
          authorization: "Bearer " + data.token
        },
        url: data.url
      })
        .then(resp => {
          resolve({ name: resp.data.name, url: "https://cdn.digherup.com/", from: "local" });
        })
        .catch(err => reject(err));
    });
}
