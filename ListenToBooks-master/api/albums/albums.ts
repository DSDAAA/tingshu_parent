import Service from "../../utils/request"
import {
  AlbumDetailInterface,
  AlbumInfoInterface,
  AlbumInfoListInterface,
  CollectTrackInterface,
  HistoryTrackInterface,
  PageResponseInterface,
  TrackStaVoInterface,
  QueryTrackInterface,
  SubscribeAlbumsInterface,
  TrackInfoInterface,
  TrackInterface,
  WorksInfoPageInterface,
  ProcessParams
} from "../albums/interfaces"

class CateGory extends Service {
  /**
   * @description 新增专辑
   * @param {AlbumInfoInterface} albumInfo
   * @return {*}
   * @docs http://139.198.163.91:8501/doc.html
   */
  addAlbum(albumInfo: AlbumInfoInterface) {
    return this.post({
      url: `/api/album/albumInfo/saveAlbumInfo`,
      data: albumInfo,
    })
  }
  /**
   * @description 修改
   * @param {AlbumInfoInterface} albumInfo
   * @return {*}
   * @docs http://139.198.163.91:8501/doc.html
   */
  editAlbum(albumInfo: AlbumInfoInterface) {
    return this.put({
      url: `/api/album/albumInfo/updateAlbumInfo`,
      data: albumInfo,
    })
  }
  /**
   * @description: 获取专辑信息
   * @param {number} id 专辑id
   * @return {*}
   * @docs http://139.198.163.91:8501/doc.html
   */
  getAlbumInfoById(id: number) {
    return this.get<AlbumInfoInterface>({
      url: `/api/album/albumInfo/getAlbumInfoById/${id}`,
    })
  }
  /**
   * @description: 删除专辑信息
   * @param {number} id 专辑id
   * @return {*}
   * @docs http://139.198.163.91:8501/doc.html
   */
  deleteAlbumInfo(id: number) {
    return this.delete({
      url: `/api/album/albumInfo/deleteAlbumInfo/${id}`,
    })
  }
  /**
   * @description: 获取声音信息
   * @param {number} id 声音id
   * @return {*}
   * @docs http://139.198.163.91:8501/doc.html
   */
  getTrackInfoById(id: number) {
    return this.get<TrackInfoInterface>({
      url: `/api/album/trackInfo/getTrackInfoById/${id}`,
    })
  }
  /**
   * @description: 删除声音信息
   * @param {number} id 声音id
   * @return {*}
   * @docs http://139.198.163.91:8501/doc.html
   */
  deleteTrackInfo(id: number) {
    return this.delete({
      url: `/api/album/trackInfo/deleteTrackInfo/${id}`,
    })
  }
  /**
   * @description: 获取当前用户的分页专辑列表
   * @param {WorksInfoPageInterface} albumInfoPage
   * @return {*}
   */
  getAlbumList(albumInfoPage:WorksInfoPageInterface) {
    return this.post<PageResponseInterface<AlbumInfoListInterface[]>>({
      url: `/api/album/albumInfo/getUserAlbumByPage/${albumInfoPage.page}/${albumInfoPage.limit}`,
      data:albumInfoPage
    })
  }
  // 获取所有的专辑列表
  /**
   * @description: 获取所有的专辑列表
   * @return {*}
   */
  getAllAlbumList() {
    return this.get<AlbumInfoListInterface[]>({
      url: `/api/album/trackInfo/findAlbumByUserId`,
    })
  }
  /**
   * @description: 新增声音信息
   * @param {TrackInfoInterface} trackInfo
   * @return {*}
   */
  addTrackInfo(trackInfo: TrackInfoInterface) {
    return this.post({
      url: `/api/album/trackInfo/saveTrackInfo`,
      data: trackInfo,
    })
  }
  /**
   * @description: 修改声音信息
   * @param {TrackInfoInterface} trackInfo
   * @return {*}
   */
  editTrackInfo(trackInfo: TrackInfoInterface) {
    return this.put({
      url: `/api/album/trackInfo/updateTrackInfoById`,
      data: trackInfo,
    })
  }
  /**
   * @description: 获取当前用户声音分页列表
   * @param {WorksInfoPageInterface} trackListInfoPage
   * @return {*}
   */
  getTrackList(trackListInfoPage:WorksInfoPageInterface) {
    return this.post<PageResponseInterface<AlbumInfoListInterface[]>>({
      url: `/api/album/trackInfo/findUserTrackPage/${trackListInfoPage.page}/${trackListInfoPage.limit}`,
      data:trackListInfoPage
    })
  }
  /**
   * @description: 获取专辑详情
   * @param {number | string} albumId
   * @return {*}
   */
  getAlbumDetail(albumId: number | string) {
    return this.get<AlbumDetailInterface>({
      url: `/api/search/albumInfo/getAlbumDetail/${albumId}`,
    })
  }
  /**
   * @description: 获取专辑声音分页列表
   * @param {QueryTrackInterface} trackListInfoPage
   * @return {*}
   */
  getAlbumTrackList(trackListInfoPage:QueryTrackInterface) {
    return this.get<PageResponseInterface<TrackInterface[]>>({
      url: `/api/album/trackInfo/getAlbumDetailTrackByPage/${trackListInfoPage.albumId}/${trackListInfoPage.page}/${trackListInfoPage.limit}`,
    })
  }

  /**
   * @description: 获取声音统计信息
   * @param {QueryTrackInterface} trackListInfoPage
   * @returns {*}
   */
  getTrackStaVo(trackId: number) {
    return this.get<TrackStaVoInterface>({
      url: `/api/album/progress/getTrackStatistics/${trackId}`
    })
  }
  /**
   * @description 获取订阅专辑分页列表
   * @param  { page: number, limit: number } pageInfo 分页信息
   */
  getSubscribeAlbums(pageInfo: { page: number, limit: number }) {
    return this.get<PageResponseInterface<SubscribeAlbumsInterface>>({
      url: `/api/album/albumInfo/getUserSubscribeByPage/${pageInfo.page}/${pageInfo.limit}`,
    })
  }
  /**
   * @description 获取收藏分页列表
   * @param  { page: number, limit: number } pageInfo 分页信息
   */
  getCollectTrack(pageInfo: { page: number, limit: number }) {
    return this.get<PageResponseInterface<CollectTrackInterface>>({
      url: `/api/album/progress/getUserCollectByPage/${pageInfo.page}/${pageInfo.limit}`,
    })
  }
  /**
   * @description 获取历史声音分页列表
   * @param  { page: number, limit: number } pageInfo 分页信息
   */
  getHistoryTrack(pageInfo: { page: number, limit: number }) {
    return this.get<PageResponseInterface<HistoryTrackInterface>>({
      url: `/api/album/progress/getPlayHistoryTrackByPage/${pageInfo.page}/${pageInfo.limit}`,
    })
  }
  /**
   * @description 订阅专辑
   */
  subscribeAlbum(albumId: number) {
    return this.get({
      url: `/api/user/userInfo/subscribe/${albumId}`,
    })
  }
  /**
   * @description 是否订阅专辑
   */
  isSubscribeAlbum(albumId: number) {
    return this.get({
      url: `/api/album/albumInfo/isSubscribe/${albumId}`,
    })
  }

  /**
   * @description 收藏声音
   */
  collectTrack(trackId: number) {
    return this.get({
      url: `/api/album/progress/collectTrack/${trackId}`,
    })
  }

  /**
   * @description 是否收藏声音
   */
  isCollectTrack(trackId: number) {
    return this.get({
      url: `/api/album/progress/isCollect/${trackId}`,
    })
  }
  /**
   * @description 根据id删除播放历史
   */
  deleteHistoryTrack(id: number) {
    return this.delete({
      url: `/api/album/progress/delete/${id}`,
    })
  }
  /**
   * @description 获取排行榜单列表
   */
  getRankingList(category1Id: number , dimension: string) {
    return this.get({
      url: `/api/search/albumInfo/getRankingList/${category1Id}/${dimension}`,
    })
  }

  /**
   * @description: 获取最近的一次播放历史
   * @returns {*}
   */
  getLatelyTrack() {
    return this.get({
      url: '/api/album/progress/getRecentlyPlay'
    })
  }

  /**
   * @description: 更新播放进度
   * @returns {*}
   */
  updateListenProcess(data: ProcessParams) {
    return this.post({
      url: '/api/album/progress/updatePlaySecond',
      data,
      loading:false
    })
  }

  /**
   * @description: 获取声音的上次跳出时间
   * @returns {*}
   */
  getTrackBreakSecond(trackId: number) {
    return this.get({
      url: `/api/album/progress/getLastPlaySecond/${trackId}`,
    })
  }

}
export const albumsService = new CateGory()
